package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.Options;
import com.hyperion.common.Utils;
import com.hyperion.dashboard.uiobject.FieldEdit;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.modules.CvPipeline;
import org.firstinspires.ftc.teamcode.modules.RectangleSampling;
import org.firstinspires.ftc.teamcode.modules.Unimetry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;
import org.openftc.revextensions2.ExpansionHubEx;

import java.io.File;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Handles all hw interfacing and robot/external initialization
 */

public class Hardware {

    public ExpansionHubEx expansionHubL;
    public ExpansionHubEx expansionHubR;
    public HardwareMap hwmp;
    public boolean isRunning;
    public Thread localizationUpdater;
    public Thread unimetryUpdater;
    public ElapsedTime autoTime;

    public Motion motion;
    public Appendages appendages;

    public LinearOpMode context;
    public String opModeID = "Choose OpMode";

    public OpenCvInternalCamera phoneCam;
    public CvPipeline cvPipeline;

    public Socket rcClient;
    public Constants constants;
    public Options options;
    public Unimetry unimetry;
    public String status = opModeID;

    public File fieldJSON;
    public File optionsJson;
    public File nnConfigJson;
    public File modelConfig;

    public DcMotor fLDrive;
    public DcMotor fRDrive;
    public DcMotor bLDrive;
    public DcMotor bRDrive;

    public DcMotor xLOdo;
    public DcMotor xROdo;
    public DcMotor yOdo;

    public DcMotor vertSlideL;
    public DcMotor vertSlideR;

    public DcMotor compWheelsL;
    public DcMotor compWheelsR;

    public Servo foundationMoverL;
    public Servo foundationMoverR;
    public Servo chainBarL;
    public Servo chainBarR;
    public Servo capstone;
    public CRServo claw;

    public Hardware(LinearOpMode context) {
        this.context = context;
        this.hwmp = context.hardwareMap;

        initFiles();

        // Init hw
        expansionHubL = hwmp.get(ExpansionHubEx.class, "Expansion Hub L");
        expansionHubR = hwmp.get(ExpansionHubEx.class, "Expansion Hub R");

        fLDrive = hwmp.dcMotor.get("fLDrive");
        fRDrive = hwmp.dcMotor.get("fRDrive");
        bLDrive = hwmp.dcMotor.get("bLDrive");
        bRDrive = hwmp.dcMotor.get("bRDrive");

        xLOdo = fLDrive;
        xROdo = fRDrive;
        yOdo = bLDrive;

        vertSlideL = hwmp.dcMotor.get("vertSlideL");
        vertSlideR = hwmp.dcMotor.get("vertSlideR");

        compWheelsL = hwmp.dcMotor.get("compWheelsL");
        compWheelsR = hwmp.dcMotor.get("compWheelsR");

        foundationMoverL = hwmp.servo.get("foundationMoverL");
        foundationMoverR = hwmp.servo.get("foundationMoverR");
        chainBarL = hwmp.servo.get("chainBarL");
        chainBarR = hwmp.servo.get("chainBarR");
        capstone = hwmp.servo.get("capstone");
        claw = hwmp.crservo.get("claw");

        // Init dashboard
        if (options.debug) initDashboard();

        // Init control, telemetry, & settings
        motion = new Motion(this);
        appendages = new Appendages(this);
        unimetry = new Unimetry(this);

        initCV();
        initUpdaters();
    }

    ///////////////////////// INIT //////////////////////////

    // Initialize localizationUpdater and unimetryUpdater threads
    public void initUpdaters() {
        localizationUpdater = new Thread(() -> {
            long lastUpdateTime = System.currentTimeMillis();
            while (!localizationUpdater.isInterrupted() && localizationUpdater.isAlive() && !context.isStopRequested()) {
                if (System.currentTimeMillis() - lastUpdateTime >= constants.LOCALIZATION_DELAY) {
                    lastUpdateTime = System.currentTimeMillis();
                    motion.localizer.update();
                }
            }
        });
        localizationUpdater.start();

        unimetryUpdater = new Thread(() -> {
            long lastUpdateTime = System.currentTimeMillis();
            while (!unimetryUpdater.isInterrupted() && unimetryUpdater.isAlive() && !context.isStopRequested()) {
                if (System.currentTimeMillis() - lastUpdateTime >= constants.UNIMETRY_DELAY) {
                    lastUpdateTime = System.currentTimeMillis();
                    unimetry.update();
                }
            }
        });
        unimetryUpdater.start();
    }

    // Initialize CV pipeline
    public void initCV() {
        int cameraMonitorViewId = hwmp.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hwmp.appContext.getPackageName());
        phoneCam = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
        phoneCam.openCameraDevice();
        cvPipeline = new RectangleSampling();
        phoneCam.setPipeline(cvPipeline);
        phoneCam.startStreaming(1280, 720, OpenCvCameraRotation.SIDEWAYS_LEFT);
        phoneCam.setFlashlightEnabled(true);
        for (OpenCvInternalCamera.FrameTimingRange r : phoneCam.getFrameTimingRangesSupportedByHardware()) {
            if (r.max == 30 && r.min == 30) {
                phoneCam.setHardwareFrameTimingRange(r);
                break;
            }
        }
    }

    // Initialize dashboard RC client socket
    public void initDashboard() {
        try {
            rcClient = IO.socket(constants.ADDRESS);

            rcClient.on(Socket.EVENT_CONNECT, args -> {
                Utils.printSocketLog("RC", "SERVER", "connected", options);
            }).on(Socket.EVENT_DISCONNECT, args -> {
                Utils.printSocketLog("RC", "SERVER", "disconnected", options);
            }).on("fieldEdited", args -> {
                Utils.printSocketLog("SERVER", "RC", "fieldEdited", options);
                writeFieldEditsToFieldJSON(args[0].toString());
            }).on("constantsUpdated", args -> {
                try {
                    Utils.printSocketLog("SERVER", "RC", "constantsUpdated", options);
                    constants.read(new JSONObject(args[0].toString()));
                    constants.write();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                initFiles();
            });

            rcClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Exactly what the method header says
    public void writeFieldEditsToFieldJSON(String json) {
        try {
            JSONObject field = new JSONObject(Utils.readFile(fieldJSON));
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                FieldEdit edit = new FieldEdit(arr.getJSONObject(i));
                if (!edit.id.equals("robot")) {
                    String o = edit.id.contains("waypoint") ? "waypoints" : "splines";
                    JSONObject target = field.getJSONObject(o);
                    switch (edit.type) {
                        case CREATE:
                        case EDIT_BODY:
                            target.put(edit.id, o.equals("waypoints") ? new JSONArray(edit.body) : new JSONObject(edit.body));
                            break;
                        case EDIT_ID:
                            if (edit.id.contains("waypoint")) {
                                JSONArray wpArr = target.getJSONArray(edit.id);
                                target.remove(edit.id);
                                target.put(edit.body, wpArr);
                            } else {
                                JSONObject splineObj = target.getJSONObject(edit.id);
                                target.remove(edit.id);
                                target.put(edit.body, splineObj);
                            }
                            break;
                        case DELETE:
                            target.remove(edit.id);
                            break;
                    }
                    field.put(o, target);
                }
            }
            Utils.writeFile(field.toString(), fieldJSON);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initFiles();
    }

    // Initialize OpMode
    public void initOpMode(String opModeID) {
        isRunning = true;
        this.opModeID = opModeID;
        status = "Running " + opModeID;

        Pose startPose = motion.waypoints.get(opModeID + ".waypoint.start");
        if (startPose == null) startPose = new Pose();
        motion.start = new RigidBody(startPose);
        motion.robot = new RigidBody(motion.start);
    }

    ///////////////////////// GENERAL & PRESETS //////////////////////////

    // Init all files & resources
    public void initFiles() {
        try {
            constants = new Constants(new File(hwmp.appContext.getFilesDir() + "/hyperilibs/data/constants.json"));
            fieldJSON = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/data/field.json");
            optionsJson = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/data/options.json");
            options = new Options(optionsJson);
            nnConfigJson = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/model/config.json");
            modelConfig = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/model/model.config");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Choose between two values depending on opMode color
    public double choose(double blue, double red) {
        return opModeID.contains("blue") ? blue : red;
    }

    ///////////////////////// END //////////////////////////

    public void killCV() {
        if (phoneCam != null) {
            phoneCam.setFlashlightEnabled(false);
            phoneCam.pauseViewport();
            phoneCam.stopStreaming();
            phoneCam.setPipeline(null);
            phoneCam.closeCameraDevice();
            System.gc();
        }
    }

    // Wrap up OpMode
    public void end() {
        if (opModeID.contains("auto") && motion.robot.pose.distanceTo(motion.getWaypoint("park")) > 3) {
            motion.pidMove("park");
        }

        status = "Ending";
        isRunning = false;

        if (localizationUpdater != null && localizationUpdater.isAlive() && !localizationUpdater.isInterrupted())
            localizationUpdater.interrupt();
        if (unimetryUpdater != null && unimetryUpdater.isAlive() && !unimetryUpdater.isInterrupted())
            unimetryUpdater.interrupt();

        try {
            if (opModeID.startsWith("auto")) {
                JSONObject obj = new JSONObject(Utils.readFile(fieldJSON));
                JSONObject wpObj = obj.getJSONObject("waypoints");
                String key = "tele." + (opModeID.contains("red") ? "red" : "blue") + ".waypoint.start";
                JSONArray wpArr = new JSONArray(motion.robot.pose.toArray());
                wpObj.put(key, wpArr);
                obj.put("waypoints", wpObj);
                Utils.writeFile(obj.toString(), fieldJSON);
            }
            if (options.debug) {
                rcClient.emit("opModeEnded", "{}");
                Utils.printSocketLog("RC", "SERVER", "opModeEnded", options);
                rcClient.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!context.isStopRequested() || context.opModeIsActive()) {
            context.requestOpModeStop();
        }
    }

}