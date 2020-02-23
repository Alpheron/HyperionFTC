package com.hyperion.common;

import com.hyperion.motion.math.Pose;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.net.InetAddress;

public class Constants {

    public static File file;
    public static JSONObject root;

    // Localization
    public static Pose XL_REL; // cm, cm, degrees
    public static Pose XR_REL; // cm, cm, degrees
    public static Pose Y_REL ; // cm, cm, degrees
    public static double TRACK_WIDTH; // cm
    public static double DRIVE_BASE; // cm
    public static double ODO_WHEEL_RADIUS; // cm
    public static double ODO_WHEEL_CIRCUMFERENCE; // cm
    public static int ODO_CYCLES_PER_REV; // cycles
    public static int ODO_CPR; // counts
    public static double COUNTS_PER_M; // counts
    public static double FIELD_SIDE_LENGTH; // cm
    public static double COORD_AXIS_LENGTH_UNITS; // coords
    public static double M_PER_COORD; // cm

    // PID
    public static double X_K_P; // coefficient
    public static double X_K_I; // coefficient
    public static double X_K_D; // coefficient
    public static double Y_K_P; // coefficient
    public static double Y_K_I; // coefficient
    public static double Y_K_D; // coefficient
    public static double THETA_K_P; // coefficient
    public static double THETA_K_I; // coefficient
    public static double THETA_K_D; // coefficient

    // Motion Profile
    public static double MP_K_TA; // coefficient
    public static double MP_K_TV; // coefficient
    public static double MP_K_AA; // coefficient
    public static double MP_K_AV; // coefficient
    public static double MAX_SEGMENT_LENGTH; // cm
    public static double MAX_BISECTION_ERROR; // cm
    public static double MAX_TRANSLATIONAL_VELOCITY; // cm/s
    public static double MAX_TRANSLATIONAL_ACCELERATION; // cm/s^2
    public static double MAX_ANGULAR_VELOCITY; // rad/s
    public static double MAX_ANGULAR_ACCELERATION; // rad/s^2
    public static double END_TRANSLATION_ERROR_THRESHOLD; // coords
    public static double END_ROTATION_ERROR_THRESHOLD; // degrees

    // I/O
    public static File RES_PREFIX; // file
    public static File RES_DATA_PREFIX; // file
    public static File RES_IMG_PREFIX; // file

    // TeamCode
    public static long LOCALIZATION_DELAY; // ms
    public static long UNIMETRY_DELAY; // ms
    public static long FORCE_END_TIME_LEFT; // ms

    // Dashboard
    public static String HOST_IP; // IP Address
    public static int PORT; // Port
    public static String ADDRESS; // Web Address
    public static String DASHBOARD_VERSION;
    public static double WAYPOINT_SIZE; // pixels
    public static double PLANNINGPOINT_SIZE; // pixels
    public static double PATHPOINT_SIZE; // pixels
    public static double WBB_STROKE_WIDTH; // pixels
    public static double WBB_GRAY_SCALE; // pixels

    public static void init(File file) {
        try {
            Constants.file = file;
            JSONTokener tokener = new JSONTokener(Utils.readFile(file));
            Constants.root = new JSONObject(tokener);
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void init() {
        try {
            JSONObject localization = root.getJSONObject("localization");
            JSONObject odometryPoses = localization.getJSONObject("odometryPoses");
            JSONArray xLRelArr = odometryPoses.getJSONArray("xLRelativePose");
            XL_REL = new Pose(xLRelArr.getDouble(0), xLRelArr.getDouble(1), Math.toRadians(xLRelArr.getDouble(2)));
            JSONArray xRRelArr = odometryPoses.getJSONArray("xRRelativePose");
            XR_REL = new Pose(xRRelArr.getDouble(0), xRRelArr.getDouble(1), Math.toRadians(xRRelArr.getDouble(2)));
            JSONArray yRelArr = odometryPoses.getJSONArray("yRelativePose");
            Y_REL = new Pose(yRelArr.getDouble(0), yRelArr.getDouble(1), Math.toRadians(yRelArr.getDouble(2)));

            TRACK_WIDTH = localization.getDouble("trackWidth");
            DRIVE_BASE = localization.getDouble("driveBase");
            ODO_WHEEL_RADIUS = localization.getDouble("odometryWheelRadius");
            ODO_WHEEL_CIRCUMFERENCE = 2 * Math.PI * ODO_WHEEL_RADIUS;
            ODO_CYCLES_PER_REV = localization.getInt("odometryCyclesPerRevolution");
            ODO_CPR = ODO_CYCLES_PER_REV * 4;
            COUNTS_PER_M = ODO_CPR / ODO_WHEEL_CIRCUMFERENCE;
            FIELD_SIDE_LENGTH = localization.getDouble("fieldSideLength");
            COORD_AXIS_LENGTH_UNITS = FIELD_SIDE_LENGTH;
            M_PER_COORD = FIELD_SIDE_LENGTH / COORD_AXIS_LENGTH_UNITS;

            JSONObject pid = root.getJSONObject("pid");
            JSONObject x = pid.getJSONObject("x");
            X_K_P = x.getDouble("kP");
            X_K_I = x.getDouble("kI");
            X_K_D = x.getDouble("kD");
            JSONObject y = pid.getJSONObject("y");
            Y_K_P = y.getDouble("kP");
            Y_K_I = y.getDouble("kI");
            Y_K_D = y.getDouble("kD");
            JSONObject theta = pid.getJSONObject("theta");
            THETA_K_P = theta.getDouble("kP");
            THETA_K_I = theta.getDouble("kI");
            THETA_K_D = theta.getDouble("kD");

            JSONObject splinesMotionProfile = root.getJSONObject("splinesMotionProfile");
            JSONObject k = splinesMotionProfile.getJSONObject("k");
            MP_K_TA = k.getDouble("kTa");
            MP_K_TV = k.getDouble("kTv");
            MP_K_AA = k.getDouble("kAa");
            MP_K_AV = k.getDouble("kAv");
            JSONObject maxes = splinesMotionProfile.getJSONObject("maxes");
            MAX_ANGULAR_VELOCITY = maxes.getDouble("aVel");
            MAX_ANGULAR_ACCELERATION = maxes.getDouble("aAcc");
            MAX_TRANSLATIONAL_VELOCITY = maxes.getDouble("tVel");
            MAX_TRANSLATIONAL_ACCELERATION = maxes.getDouble("tAcc");
            MAX_SEGMENT_LENGTH = maxes.getDouble("segmentLength");
            MAX_BISECTION_ERROR = maxes.getDouble("bisectionError");
            JSONObject endErrorThresholds = splinesMotionProfile.getJSONObject("endErrorThresholds");
            END_TRANSLATION_ERROR_THRESHOLD = endErrorThresholds.getDouble("translation");
            END_ROTATION_ERROR_THRESHOLD = Math.toRadians(endErrorThresholds.getDouble("rotation"));

            JSONObject io = root.getJSONObject("io");
            JSONObject filePaths = io.getJSONObject("filePaths");
            RES_PREFIX = new File(System.getProperty("user.dir") + filePaths.getString("resPrefix"));
            RES_DATA_PREFIX = new File(RES_PREFIX + filePaths.getString("resDataPrefix"));
            RES_IMG_PREFIX = new File(RES_PREFIX + filePaths.getString("resImgPrefix"));

            JSONObject teamcode = root.getJSONObject("teamcode");
            JSONObject slidesPreset = teamcode.getJSONObject("slidesPreset");
            LOCALIZATION_DELAY = teamcode.getLong("localizationDelay");
            UNIMETRY_DELAY = teamcode.getLong("unimetryDelay");
            FORCE_END_TIME_LEFT = teamcode.getLong("forceEndTimeLeft");

            JSONObject dashboard = root.getJSONObject("dashboard");
            DASHBOARD_VERSION = dashboard.getString("version");
            JSONObject net = dashboard.getJSONObject("net");
            HOST_IP = net.getString("hostIP");
            if (HOST_IP.equals("this")) {
                HOST_IP = InetAddress.getLocalHost().getHostAddress();
            }
            System.out.println("Host IP: " + HOST_IP);
            PORT = net.getInt("port");
            ADDRESS = "http://" + HOST_IP + ":" + PORT;
            JSONObject gui = dashboard.getJSONObject("gui");
            JSONObject sizes = gui.getJSONObject("sizes");
            WAYPOINT_SIZE = sizes.getDouble("waypoint");
            PLANNINGPOINT_SIZE = sizes.getDouble("planningPoint");
            PATHPOINT_SIZE = sizes.getDouble("pathPoint");
            JSONObject wbb = gui.getJSONObject("wbb");
            WBB_STROKE_WIDTH = wbb.getDouble("strokeWidth");
            WBB_GRAY_SCALE = wbb.getDouble("grayScale");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void read(JSONObject root) {
        Constants.root = root;
        init();
    }

    public static JSONObject toJSONObject() {
        JSONObject root = new JSONObject();
        try {
            JSONObject localization = new JSONObject();
            JSONObject odometryPoses = new JSONObject();
            odometryPoses.put("xLRelativePose", new JSONArray(XL_REL.toArray()));
            odometryPoses.put("xRRelativePose", new JSONArray(XR_REL.toArray()));
            double[] yrel = Y_REL.toArray();
            yrel[2] = Math.toDegrees(yrel[2]);
            odometryPoses.put("yRelativePose", new JSONArray(yrel));
            localization.put("odometryPoses", odometryPoses);

            localization.put("trackWidth", TRACK_WIDTH);
            localization.put("driveBase", DRIVE_BASE);
            localization.put("odometryWheelRadius", ODO_WHEEL_RADIUS);
            localization.put("odometryCyclesPerRevolution", ODO_CYCLES_PER_REV);
            localization.put("fieldSideLength", FIELD_SIDE_LENGTH);
            root.put("localization", localization);

            JSONObject pid = new JSONObject();
            JSONObject x = new JSONObject();
            x.put("kP", X_K_P);
            x.put("kI", X_K_I);
            x.put("kD", X_K_D);
            pid.put("x", x);
            JSONObject y = new JSONObject();
            y.put("kP", Y_K_P);
            y.put("kI", Y_K_I);
            y.put("kD", Y_K_D);
            pid.put("y", y);
            JSONObject theta = new JSONObject();
            theta.put("kP", THETA_K_P);
            theta.put("kI", THETA_K_I);
            theta.put("kD", THETA_K_D);
            pid.put("theta", theta);
            root.put("pid", pid);

            JSONObject splinesMotionProfile = new JSONObject();
            JSONObject k = new JSONObject();
            k.put("kTv", MP_K_TV);
            k.put("kTa", MP_K_TA);
            k.put("kAa", MP_K_AA);
            k.put("kAv", MP_K_AV);
            splinesMotionProfile.put("k", k);
            JSONObject maxes = new JSONObject();
            maxes.put("segmentLength", MAX_SEGMENT_LENGTH);
            maxes.put("bisectionError", MAX_BISECTION_ERROR);
            maxes.put("tVel", MAX_TRANSLATIONAL_VELOCITY);
            maxes.put("tAcc", MAX_TRANSLATIONAL_ACCELERATION);
            maxes.put("aVel", MAX_ANGULAR_VELOCITY);
            maxes.put("aAcc", MAX_ANGULAR_ACCELERATION);
            splinesMotionProfile.put("maxes", maxes);
            JSONObject endErrorThresholds = new JSONObject();
            endErrorThresholds.put("translation", END_TRANSLATION_ERROR_THRESHOLD);
            endErrorThresholds.put("rotation", Math.toDegrees(END_ROTATION_ERROR_THRESHOLD));
            splinesMotionProfile.put("endErrorThresholds", endErrorThresholds);
            root.put("splinesMotionProfile", splinesMotionProfile);

            JSONObject io = new JSONObject();
            JSONObject filePaths = new JSONObject();
            filePaths.put("resPrefix", RES_PREFIX.toString().replace(System.getProperty("user.dir"), ""));
            filePaths.put("resDataPrefix", RES_DATA_PREFIX.toString().replace(RES_PREFIX.toString(), ""));
            filePaths.put("resImgPrefix", RES_IMG_PREFIX.toString().replace(RES_PREFIX.toString(), ""));
            io.put("filePaths", filePaths);
            root.put("io", io);

            JSONObject teamcode = new JSONObject();
            JSONObject slidesPreset = new JSONObject();
            teamcode.put("localizationDelay", LOCALIZATION_DELAY);
            teamcode.put("unimetryDelay", UNIMETRY_DELAY);
            teamcode.put("forceEndTimeLeft", FORCE_END_TIME_LEFT);
            teamcode.put("slidesPreset", slidesPreset);
            root.put("teamcode", teamcode);

            JSONObject dashboard = new JSONObject();
            dashboard.put("version", DASHBOARD_VERSION);
            JSONObject net = new JSONObject();
            net.put("hostIP", HOST_IP);
            net.put("port", PORT);
            dashboard.put("net", net);
            JSONObject gui = new JSONObject();
            JSONObject sizes = new JSONObject();
            sizes.put("waypoint", WAYPOINT_SIZE);
            sizes.put("planningPoint", PLANNINGPOINT_SIZE);
            sizes.put("pathPoint", PATHPOINT_SIZE);
            gui.put("sizes", sizes);
            JSONObject wbb = new JSONObject();
            wbb.put("strokeWidth", WBB_STROKE_WIDTH);
            wbb.put("grayScale", WBB_GRAY_SCALE);
            gui.put("wbb", wbb);
            dashboard.put("gui", gui);
            root.put("dashboard", dashboard);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return root;
    }

    public static void write() {
        try {
            root = toJSONObject();
            Utils.writeFile(root.toString(4), file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double mToCoords(double m) {
        return m / M_PER_COORD;
    } // coords
    public static double countsToM(double counts) {
        return counts / COUNTS_PER_M;
    } // m

    public String toString() {
        return toJSONObject().toString();
    }
}
