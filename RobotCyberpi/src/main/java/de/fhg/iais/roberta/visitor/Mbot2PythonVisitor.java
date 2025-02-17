package de.fhg.iais.roberta.visitor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ClassToInstanceMap;

import de.fhg.iais.roberta.bean.CodeGeneratorSetupBean;
import de.fhg.iais.roberta.bean.IProjectBean;
import de.fhg.iais.roberta.bean.UsedHardwareBean;
import de.fhg.iais.roberta.components.ConfigurationAst;
import de.fhg.iais.roberta.components.UsedSensor;
import de.fhg.iais.roberta.constants.CyberpiConstants;
import de.fhg.iais.roberta.inter.mode.action.IDriveDirection;
import de.fhg.iais.roberta.syntax.Phrase;
import de.fhg.iais.roberta.syntax.action.display.ClearDisplayAction;
import de.fhg.iais.roberta.syntax.action.display.ShowTextAction;
import de.fhg.iais.roberta.syntax.action.light.LedsOffAction;
import de.fhg.iais.roberta.syntax.action.mbot2.CommunicationReceiveAction;
import de.fhg.iais.roberta.syntax.action.mbot2.CommunicationSendAction;
import de.fhg.iais.roberta.syntax.action.mbot2.DisplaySetColourAction;
import de.fhg.iais.roberta.syntax.action.mbot2.LedBrightnessAction;
import de.fhg.iais.roberta.syntax.action.mbot2.LedOnActionWithIndex;
import de.fhg.iais.roberta.syntax.action.mbot2.PlayRecordingAction;
import de.fhg.iais.roberta.syntax.action.mbot2.PrintlnAction;
import de.fhg.iais.roberta.syntax.action.mbot2.QuadRGBLightOffAction;
import de.fhg.iais.roberta.syntax.action.mbot2.QuadRGBLightOnAction;
import de.fhg.iais.roberta.syntax.action.mbot2.Ultrasonic2LEDAction;
import de.fhg.iais.roberta.syntax.action.motor.MotorGetPowerAction;
import de.fhg.iais.roberta.syntax.action.motor.MotorOnAction;
import de.fhg.iais.roberta.syntax.action.motor.MotorSetPowerAction;
import de.fhg.iais.roberta.syntax.action.motor.MotorStopAction;
import de.fhg.iais.roberta.syntax.action.motor.differential.CurveAction;
import de.fhg.iais.roberta.syntax.action.motor.differential.DriveAction;
import de.fhg.iais.roberta.syntax.action.motor.differential.MotorDriveStopAction;
import de.fhg.iais.roberta.syntax.action.motor.differential.TurnAction;
import de.fhg.iais.roberta.syntax.action.sound.GetVolumeAction;
import de.fhg.iais.roberta.syntax.action.sound.PlayFileAction;
import de.fhg.iais.roberta.syntax.action.sound.PlayNoteAction;
import de.fhg.iais.roberta.syntax.action.sound.SetVolumeAction;
import de.fhg.iais.roberta.syntax.action.sound.ToneAction;
import de.fhg.iais.roberta.syntax.configuration.ConfigurationComponent;
import de.fhg.iais.roberta.syntax.lang.blocksequence.MainTask;
import de.fhg.iais.roberta.syntax.lang.expr.ColorConst;
import de.fhg.iais.roberta.syntax.lang.expr.Expr;
import de.fhg.iais.roberta.syntax.lang.expr.RgbColor;
import de.fhg.iais.roberta.syntax.lang.stmt.StmtList;
import de.fhg.iais.roberta.syntax.lang.stmt.WaitStmt;
import de.fhg.iais.roberta.syntax.lang.stmt.WaitTimeStmt;
import de.fhg.iais.roberta.syntax.sensor.generic.AccelerometerSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.EncoderReset;
import de.fhg.iais.roberta.syntax.sensor.generic.EncoderSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.GetLineSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.GyroSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.KeysSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.LightSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.SoundSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.TimerReset;
import de.fhg.iais.roberta.syntax.sensor.generic.TimerSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.UltrasonicSensor;
import de.fhg.iais.roberta.syntax.sensor.mbot2.GyroResetAxis;
import de.fhg.iais.roberta.syntax.sensor.mbot2.Joystick;
import de.fhg.iais.roberta.syntax.sensor.mbot2.QuadRGBSensor;
import de.fhg.iais.roberta.syntax.sensor.mbot2.SoundRecord;
import de.fhg.iais.roberta.util.dbc.Assert;
import de.fhg.iais.roberta.util.dbc.DbcException;
import de.fhg.iais.roberta.util.syntax.MotionParam;
import de.fhg.iais.roberta.util.syntax.MotorDuration;
import de.fhg.iais.roberta.util.syntax.SC;
import de.fhg.iais.roberta.visitor.lang.codegen.prog.AbstractPythonVisitor;

/**
 * This class is implementing {@link IVisitor}. All methods are implemented and they append a human-readable Python code representation of a phrase to a
 * StringBuilder. <b>This representation is correct Python code.</b> <br>
 */
public final class Mbot2PythonVisitor extends AbstractPythonVisitor implements IMbot2Visitor<Void> {

    private final ConfigurationAst configurationAst;
    private String rightMotorPort;

    /**
     * initialize the Python code generator visitor.
     *
     * @param programPhrases to generate the code from
     */
    public Mbot2PythonVisitor(
        List<List<Phrase>> programPhrases, ClassToInstanceMap<IProjectBean> beans, ConfigurationAst configurationAst) {
        super(programPhrases, beans);
        this.configurationAst = configurationAst;
        setRightMotorPort();
    }


    @Override
    protected void generateProgramPrefix(boolean withWrapping) {
        if ( !withWrapping ) {
            return;
        }
        this.src.add("import cyberpi");
        if ( this.getBean(UsedHardwareBean.class).isActorUsed(SC.ENCODER) ) {
            this.src.add(", mbot2");
        }
        if ( this.getBean(UsedHardwareBean.class).isSensorUsed(CyberpiConstants.MBUILDSENSOR) ) {
            this.src.add(", mbuild");
        }
        nlIndent();
        this.src.add("import time");
        nlIndent();
        this.src.add("import math, random");
        nlIndent();
        if ( this.getBean(UsedHardwareBean.class).isActorUsed(SC.DIFFERENTIALDRIVE) ) {
            appendRobotVariables();
        }
        generateTimerVariables();
        addColors();
        if ( !this.getBean(CodeGeneratorSetupBean.class).getUsedMethods().isEmpty() ) {
            String helperMethodImpls =
                this.getBean(CodeGeneratorSetupBean.class)
                    .getHelperMethodGenerator()
                    .getHelperMethodDefinitions(this.getBean(CodeGeneratorSetupBean.class).getUsedMethods());
            this.src.add(helperMethodImpls);
        }
    }

    private void appendRobotVariables() {
        ConfigurationComponent diffDrive = getDiffDrive();
        if ( diffDrive != null ) {
            nlIndent();
            double circumference = Double.parseDouble(diffDrive.getComponentProperties().get(CyberpiConstants.DIFF_WHEEL_DIAMETER)) * Math.PI;
            double trackWidth = Double.parseDouble(diffDrive.getComponentProperties().get(CyberpiConstants.DIFF_TRACK_WIDTH));
            this.src.add("_trackWidth = ");
            this.src.add(trackWidth);
            nlIndent();
            this.src.add("_circumference = ");
            this.src.add(circumference);
            nlIndent();
            this.src.add("_diffPortsSwapped = ");
            this.src.add(this.rightMotorPort.equals("EM1") ? "True" : "False");
            nlIndent();
        }
    }

    private void generateTimerVariables() {
        this.getBean(UsedHardwareBean.class)
            .getUsedSensors()
            .stream()
            .filter(usedSensor -> usedSensor.getType().equals(SC.TIMER))
            .collect(Collectors.groupingBy(UsedSensor::getPort))
            .keySet()
            .forEach(port -> {
                this.usedGlobalVarInFunctions.add("_timer" + port);
                this.src.add("_timer", port, " = cyberpi.timer.get()");
                nlIndent();
            });
    }

    private void addColors() {
        int colorBlocks = (int) this.getBean(UsedHardwareBean.class).getUsedSensors()
            .stream()
            .filter(usedSensor -> usedSensor.getType().equals(CyberpiConstants.QUADRGB)
                && usedSensor.getMode().equals(SC.COLOUR) || usedSensor.getMode().equals(SC.LED))
            .count();

        if ( colorBlocks != 0 ) {
            nlIndent();
            this.src.add("_colors = {\n",
                "            \"red\": (204,0,0),\n",
                "            \"yellow\": (255,255,0),\n",
                "            \"green\": (51,204,0),\n",
                "            \"cyan\": (51,255,255),\n",
                "            \"blue\": (51,102,255),\n",
                "            \"purple\": (204,51,204),\n",
                "            \"white\": (255,255,255),\n",
                "            \"black\": (0,0,0)\n",
                "        }");
            nlIndent();
        }
    }


    @Override
    public Void visitMainTask(MainTask mainTask) {
        StmtList variables = mainTask.variables;
        variables.accept(this);
        generateUserDefinedMethods();
        nlIndent();
        this.src.add("def run():");
        incrIndentation();
        if ( !this.usedGlobalVarInFunctions.isEmpty() ) {
            nlIndent();
            this.src.add("global ", String.join(", ", this.usedGlobalVarInFunctions));
        }

        return null;
    }

    @Override
    protected void generateProgramSuffix(boolean withWrapping) {
        if ( !withWrapping ) {
            return;
        }
        decrIndentation(); // everything is still indented from main program
        nlIndent();
        nlIndent();
        this.src.add("def main():");
        incrIndentation();
        nlIndent();
        this.src.add("try:");
        incrIndentation();
        nlIndent();
        this.src.add("run()");
        decrIndentation();
        nlIndent();
        this.src.add("except Exception as e:");
        incrIndentation();
        nlIndent();
        this.src.add("cyberpi.display.show_label(\"Exeption on Mbot 2\", 16, int(8 * 0 + 5), int(17 * 0))");
        nlIndent();
        this.src.add("cyberpi.display.show_label(e, 16, int(8 * 0 + 5), int(17 * 1))");
        nlIndent();
        this.src.add("raise");
        decrIndentation();
        if ( this.getBean(UsedHardwareBean.class).isActorUsed(SC.ENCODER) ) {
            nlIndent();
            this.src.add("finally:");
            incrIndentation();
            nlIndent();
            this.src.add("mbot2.motor_stop(\"all\")");
            nlIndent();
            this.src.add("mbot2.EM_stop(\"all\")");
            decrIndentation();
        }
        decrIndentation();
        nlIndent();

        this.src.add("main()");
    }

    @Override
    public Void visitKeysSensor(KeysSensor keysSensor) {
        this.src.add("cyberpi.controller.is_press(");
        String port = keysSensor.getUserDefinedPort();
        ConfigurationComponent configurationComponent = this.configurationAst.getConfigurationComponent(port);
        String pin1 = configurationComponent.getProperty("PIN1");

        this.src.add("\"", pin1, "\"");
        this.src.add(")");
        return null;
    }

    @Override
    public Void visitJoystick(Joystick joystick) {
        this.src.add("cyberpi.controller.is_press(\"");
        String slot = joystick.getSlot().toLowerCase();
        if ( slot.equals("center") ) {
            this.src.add("middle");
        } else if ( slot.equals("any") ) {
            this.src.add("any_direction");
        } else {
            this.src.add(slot);
        }
        this.src.add("\")");
        return null;
    }

    @Override
    public Void visitClearDisplayAction(ClearDisplayAction clearDisplayAction) {
        this.src.add("cyberpi.console.clear()");
        return null;
    }

    @Override
    public Void visitLedOnActionWithIndex(LedOnActionWithIndex ledOnActionWithIndex) {
        this.src.add("cyberpi.led.on(");
        appendRGBAsArguments(ledOnActionWithIndex.color);
        this.src.add(", ");
        appendLedNumber(ledOnActionWithIndex.led);
        this.src.add(")");
        return null;
    }

    @Override
    public Void visitCommunicationSendAction(CommunicationSendAction communicationSendAction) {
        this.src.add("cyberpi.wifi_broadcast.set(");
        communicationSendAction.channel.accept(this);
        this.src.add(", ");
        communicationSendAction.message.accept(this);
        this.src.add(")");
        return null;
    }

    @Override
    public Void visitCommunicationReceiveAction(CommunicationReceiveAction communicationReceiveAction) {
        this.src.add("cyberpi.wifi_broadcast.get(");
        communicationReceiveAction.channel.accept(this);
        this.src.add(")");
        return null;
    }

    @Override
    public Void visitLedsOffAction(LedsOffAction ledsOffAction) {
        this.src.add("cyberpi.led.off(");
        appendLedNumber(ledsOffAction.led);
        this.src.add(")");
        return null;
    }

    @Override
    public Void visitLedBrightnessAction(LedBrightnessAction ledBrightnessAction) {
        this.src.add("cyberpi.led.set_bri(");
        ledBrightnessAction.brightness.accept(this);
        this.src.add(")");
        return null;
    }

    private void appendLedNumber(String led) {
        led = led.replace("LED", "").toLowerCase();
        if ( led.equals("all") ) {
            this.src.add("\"", led, "\"");
        } else {
            this.src.add(led);
        }
    }

    private String getPortFromConfig(String name) {
        ConfigurationComponent block = configurationAst.getConfigurationComponent(name);
        return block.getComponentProperties().get("PORT1");
    }

    @Override
    public Void visitMotorGetPowerAction(MotorGetPowerAction motorGetPowerAction) {
        return null;
    }

    @Override
    public Void visitMotorSetPowerAction(MotorSetPowerAction motorSetPowerAction) {
        return null;
    }

    @Override
    public Void visitMotorStopAction(MotorStopAction motorStopAction) {
        String port = getPortFromConfig(motorStopAction.getUserDefinedPort());
        this.src.add("mbot2.EM_stop(\"", port, "\")");
        return null;
    }

    @Override
    public Void visitPrintlnAction(PrintlnAction printlnAction) {
        this.src.add("cyberpi.console.println(");
        printlnAction.msg.accept(this);
        this.src.add(")");
        return null;
    }

    @Override
    public Void visitShowTextAction(ShowTextAction showTextAction) {
        this.src.add("cyberpi.display.show_label(");
        showTextAction.msg.accept(this);
        this.src.add(", 16, ");
        this.src.add("int(8 * ");
        showTextAction.x.accept(this);
        this.src.add(" + 5), ");
        this.src.add("int(17 * ");
        showTextAction.y.accept(this);
        this.src.add("))");
        return null;
    }

    @Override
    public Void visitPlayRecordingAction(PlayRecordingAction playRecordingAction) {
        this.src.add("cyberpi.audio.play_record()");
        return null;
    }

    @Override
    public Void visitDisplaySetColourAction(DisplaySetColourAction displaySetColourAction) {
        this.src.add("cyberpi.display.set_brush(");
        appendRGBAsArguments(displaySetColourAction.color);
        this.src.add(")");
        return null;
    }

    private void appendRGBAsArguments(Expr color) {
        color.accept(this);
        this.src.add("[0], ");
        color.accept(this);
        this.src.add("[1], ");
        color.accept(this);
        this.src.add("[2]");
    }

    @Override
    public Void visitUltrasonicSensor(UltrasonicSensor ultrasonicSensor) {
        int index = getSensorNumber(CyberpiConstants.ULTRASONIC2, ultrasonicSensor.getUserDefinedPort());
        this.src.add("mbuild.ultrasonic2.get(", index, ")");
        return null;
    }

    @Override
    public Void visitQuadRGBSensor(QuadRGBSensor quadRGBSensor) {
        int index = getSensorNumber(CyberpiConstants.MBUILD_QUADRGB, quadRGBSensor.getUserDefinedPort());
        String mode = quadRGBSensor.getMode();
        switch ( mode ) {
            case SC.LIGHT:
                this.src.add("mbuild.quad_rgb_sensor.get_gray(", "\"", quadRGBSensor.getSlot(), "\", ", index, ")");
                break;
            case SC.COLOUR:
                this.src.add("_colors[", "mbuild.quad_rgb_sensor.get_color_sta(", "\"", quadRGBSensor.getSlot(), "\", ", index, ")", "]");
                break;
            case SC.AMBIENTLIGHT:
                this.src.add("mbuild.quad_rgb_sensor.get_light(", "\"", quadRGBSensor.getSlot(), "\", ", index, ")");
                break;
            case SC.RGB:
                this.src.add("[", getRGBString("red", index, quadRGBSensor), ", ",
                    getRGBString("green", index, quadRGBSensor), ", ", getRGBString("blue", index, quadRGBSensor),
                    "]");
                break;
        }
        return null;
    }

    @Override
    public Void visitGetLineSensor(GetLineSensor getLineSensor) {
        int index = getSensorNumber(CyberpiConstants.MBUILD_QUADRGB, getLineSensor.getUserDefinedPort());
        this.src.add("mbuild.quad_rgb_sensor.get_line_sta(\"all\", ", index, ")");
        return null;
    }

    @Override
    public Void visitQuadRGBLightOnAction(QuadRGBLightOnAction quadRGBLightOnAction) {
        int index = getSensorNumber(CyberpiConstants.MBUILD_QUADRGB, quadRGBLightOnAction.getUserDefinedPort());
        this.src.add("mbuild.quad_rgb_sensor.set_led(");
        this.src.add(this.getBean(CodeGeneratorSetupBean.class).getHelperMethodGenerator().getHelperMethodName(Mbot2Methods.RGBASSTRING));
        this.src.add("(");
        quadRGBLightOnAction.color.accept(this);
        this.src.add(")");
        this.src.add(", ", index, ")");
        return null;
    }

    @Override
    public Void visitQuadRGBLightOffAction(QuadRGBLightOffAction quadRGBLightOffAction) {
        int index = getSensorNumber(CyberpiConstants.MBUILD_QUADRGB, quadRGBLightOffAction.getUserDefinedPort());
        this.src.add("mbuild.quad_rgb_sensor.off_led(", index, ")");
        return null;
    }

    @Override
    public Void visitUltrasonic2LEDAction(Ultrasonic2LEDAction ultrasonic2LEDAction) {
        int index = getSensorNumber(CyberpiConstants.ULTRASONIC2, ultrasonic2LEDAction.getUserDefinedPort());
        this.src.add("mbuild.ultrasonic2.set_bri(");
        ultrasonic2LEDAction.brightness.accept(this);
        this.src.add(", ");
        appendLedNumber(ultrasonic2LEDAction.getLedNumber());
        this.src.add(", ", index, ")");
        return null;
    }

    private String getRGBString(String colour, int index, QuadRGBSensor quadRGBSensor) {
        return "mbuild.quad_rgb_sensor.get_" + colour + "(\"" + quadRGBSensor.getSlot() + "\", " + index + ")";
    }

    private ConfigurationComponent getMbuildPort() {
        for ( Map.Entry<String, ConfigurationComponent> entry : configurationAst.getConfigurationComponents().entrySet() ) {
            ConfigurationComponent component = entry.getValue();
            if ( component.componentType.equals("MBUILD_PORT") ) {
                return component;
            }
        }
        throw new DbcException("Mbuild-port is missing");
    }

    private List<ConfigurationComponent> getMbuildModules() {
        ConfigurationComponent mbuildPort = getMbuildPort();
        return mbuildPort.getSubComponents().get(CyberpiConstants.MBUILDSENSOR);
    }

    private int getSensorNumber(String sensorName, String userDefinedName) {
        int index = 0;
        for ( ConfigurationComponent sensor : getMbuildModules() ) {
            if ( sensor.componentType.equals(sensorName) ) {
                index++;
            }
            if ( sensor.userDefinedPortName.equals(userDefinedName) ) {
                break;
            }
        }
        Assert.isTrue(index > 0, "Sensor is missing in the configuration");
        return index;
    }

    @Override
    public Void visitEncoderSensor(EncoderSensor encoderSensor) {
        String port = getPortFromConfig(encoderSensor.getUserDefinedPort());
        if ( encoderSensor.getMode().equals("ROTATION") ) {
            this.src.add("(mbot2.EM_get_angle(\"", port, "\")", " / 360)");
        } else {
            this.src.add("mbot2.EM_get_angle(\"", port, "\")");
        }
        return null;
    }

    @Override
    public Void visitEncoderReset(EncoderReset encoderReset) {
        String port = getPortFromConfig(encoderReset.sensorPort);
        this.src.add("mbot2.EM_reset_angle(\"", port, "\") ");
        return null;
    }

    @Override
    public Void visitSoundSensor(SoundSensor soundSensor) {
        this.src.add("cyberpi.get_loudness()");
        return null;
    }

    @Override
    public Void visitSoundRecord(SoundRecord soundRecord) {
        String mode = soundRecord.getMode();
        if ( mode.equals("START") ) {
            this.src.add("cyberpi.audio.record()");
        } else if ( mode.equals("STOP") ) {
            this.src.add("cyberpi.audio.stop_record()");
        } else {
            throw new DbcException("invalid mode for SoundRecord: " + mode);
        }
        return null;
    }

    @Override
    public Void visitLightSensor(LightSensor lightSensor) {
        this.src.add("cyberpi.get_bri()");
        return null;
    }

    @Override
    public Void visitGyroSensor(GyroSensor gyroSensor) {
        this.src.add("cyberpi.get_rotation(\"", gyroSensor.getSlot().toLowerCase(), "\")");
        return null;
    }

    @Override
    public Void visitGyroResetAxis(GyroResetAxis gyroResetAxis) {
        this.src.add("cyberpi.reset_rotation(\"", gyroResetAxis.getSlot().toLowerCase(), "\")");
        return null;
    }

    @Override
    public Void visitAccelerometerSensor(AccelerometerSensor accelerometerSensor) {
        this.src.add("cyberpi.get_acc(\"", accelerometerSensor.getSlot().toLowerCase(), "\")", "/ 9.80665");
        return null;
    }

    @Override
    public Void visitMotorOnAction(MotorOnAction motorOnAction) {
        MotorDuration distance = motorOnAction.param.getDuration();
        String port = getPortFromConfig(motorOnAction.getUserDefinedPort());
        if ( distance == null ) {
            this.src.add("mbot2.EM_set_speed(");
            motorOnAction.param.getSpeed().accept(this);
            this.src.add(", \"", port, "\")");
        } else {
            this.src.add("mbot2.EM_turn((");
            distance.getValue().accept(this);
            this.src.add(")");
            if ( distance.getType().toString().equals("ROTATIONS") ) {
                this.src.add(" * 360");
            }
            this.src.add(", ");
            motorOnAction.param.getSpeed().accept(this);
            this.src.add(", \"", port, "\")");
        }
        return null;
    }

    @Override
    public Void visitDriveAction(DriveAction driveAction) {
        if ( driveAction.param.getDuration() != null ) {
            appendCurveForAction(driveAction.param, driveAction.param, driveAction.param.getDuration(), driveAction.direction);
        } else {
            appendCurveAction(driveAction.param, driveAction.param, driveAction.direction);
        }

        return null;
    }

    @Override
    public Void visitMotorDriveStopAction(MotorDriveStopAction stopAction) {
        this.src.add("mbot2.EM_stop()");
        return null;
    }

    @Override
    public Void visitTurnAction(TurnAction turnAction) {
        String direction = turnAction.direction.toString().toLowerCase();

        if ( turnAction.param.getDuration() != null ) {
            appendTurnForAction(turnAction, direction);
        } else {
            appendTurnAction(turnAction, direction);
        }
        return null;
    }

    private void appendTurnAction(TurnAction turnAction, String direction) {
        String multi = "";
        String optBracket = "";
        if ( direction.equals("left") ) {
            multi = "-(";
            optBracket = ")";
        }
        this.src.add("mbot2.drive_speed(", multi);
        turnAction.param.getSpeed().accept(this);
        this.src.add(optBracket, ", ", multi);
        turnAction.param.getSpeed().accept(this);
        this.src.add(optBracket, ")");

    }

    private void appendTurnForAction(TurnAction turnAction, String direction) {
        this.src.add("mbot2.turn(");
        String multi = "";
        String optBracket = "";
        if ( direction.equals("left") ) {
            multi = "-(";
            optBracket = ")";
        }
        this.src.add(multi);
        turnAction.param.getDuration().getValue().accept(this);
        this.src.add(optBracket);
        this.src.add(", ");
        turnAction.param.getSpeed().accept(this);
        this.src.add(")");
    }

    @Override
    public Void visitCurveAction(CurveAction curveAction) {
        MotorDuration duration = curveAction.paramLeft.getDuration();
        if ( duration != null ) {
            appendCurveForAction(curveAction.paramLeft, curveAction.paramRight, curveAction.paramLeft.getDuration(), curveAction.direction);
        } else {
            appendCurveAction(curveAction.paramLeft, curveAction.paramRight, curveAction.direction);
        }

        return null;
    }

    private void appendCurveForAction(MotionParam speedL, MotionParam speedR, MotorDuration distance, IDriveDirection direction) {
        boolean isForward = direction.toString().equals(SC.FOREWARD);
        this.src.add(this.getBean(CodeGeneratorSetupBean.class).getHelperMethodGenerator().getHelperMethodName(Mbot2Methods.DIFFDRIVEFOR));
        if ( isForward ) {
            this.src.add("(");
            speedL.getSpeed().accept(this);
            this.src.add(", ");
            speedR.getSpeed().accept(this);
            this.src.add(", ");
        } else {
            this.src.add("(-(");
            speedL.getSpeed().accept(this);
            this.src.add("), -(");
            speedR.getSpeed().accept(this);
            this.src.add("), ");
        }
        distance.getValue().accept(this);
        this.src.add(")");
    }

    private void appendCurveAction(MotionParam speedLeft, MotionParam speedRight, IDriveDirection direction) {
        this.src.add("mbot2.drive_speed(");
        boolean isForward = direction.toString().equals(SC.FOREWARD);
        if ( isForward ) {
            if ( isMotorSwapped() ) {
                this.src.add("-(");
                speedRight.getSpeed().accept(this);
                this.src.add("),");
                speedLeft.getSpeed().accept(this);
            } else {
                speedLeft.getSpeed().accept(this);
                this.src.add(", -(");
                speedRight.getSpeed().accept(this);
                this.src.add(")");
            }
        } else {
            if ( isMotorSwapped() ) {
                speedRight.getSpeed().accept(this);
                this.src.add(", -(");
                speedLeft.getSpeed().accept(this);
                this.src.add(")");
            } else {
                this.src.add("-(");
                speedLeft.getSpeed().accept(this);
                this.src.add("),");
                speedRight.getSpeed().accept(this);
            }
        }
        this.src.add(")");
    }

    private ConfigurationComponent getDiffDrive() {
        for ( ConfigurationComponent component : this.configurationAst.getConfigurationComponents().values() ) {
            if ( component.componentType.equals(CyberpiConstants.DIFFERENTIALDRIVE) ) {
                return component;
            }
        }
        return null;
    }

    private void setRightMotorPort() {
        if ( rightMotorPort == null ) {
            ConfigurationComponent diffDrive = getDiffDrive();
            if ( diffDrive != null ) {
                rightMotorPort = diffDrive.getOptProperty("MOTOR_R");
            } else {
                rightMotorPort = "EM2";
            }
        }
    }

    private boolean isMotorSwapped() {
        return this.rightMotorPort.equals("EM1");
    }

    @Override
    public Void visitToneAction(ToneAction toneAction) {
        this.src.add("cyberpi.audio.play_tone(int(");
        toneAction.frequency.accept(this);
        this.src.add("), ");
        toneAction.duration.accept(this);
        this.src.add(" * 0.001)");
        return null;
    }

    @Override
    public Void visitPlayNoteAction(PlayNoteAction playNoteAction) {
        this.src.add("cyberpi.audio.play_tone(int(", playNoteAction.frequency, "), ", playNoteAction.duration, " * 0.001)");
        return null;
    }

    @Override
    public Void visitGetVolumeAction(GetVolumeAction getVolumeAction) {
        this.src.add("cyberpi.audio.get_vol()");
        return null;
    }

    @Override
    public Void visitSetVolumeAction(SetVolumeAction setVolumeAction) {
        this.src.add("cyberpi.audio.set_vol(");
        setVolumeAction.volume.accept(this);
        this.src.add(")");
        return null;
    }

    @Override
    public Void visitTimerSensor(TimerSensor timerSensor) {
        this.src.add("((cyberpi.timer.get() - _timer", timerSensor.getUserDefinedPort(), ")*1000)");
        return null;
    }

    @Override
    public Void visitTimerReset(TimerReset timerReset) {
        this.src.add("_timer", timerReset.sensorPort, " = cyberpi.timer.get()");
        return null;
    }

    @Override
    public Void visitPlayFileAction(PlayFileAction playFileAction) {
        return null;
    }

    @Override
    public Void visitWaitStmt(WaitStmt waitStmt) {
        this.src.add("while True:");
        incrIndentation();
        visitStmtList(waitStmt.statements);
        decrIndentation();
        return null;
    }

    @Override
    public Void visitWaitTimeStmt(WaitTimeStmt waitTimeStmt) {
        this.src.add("time.sleep(");
        waitTimeStmt.time.accept(this);
        this.src.add("/1000)");
        return null;
    }

    @Override
    public Void visitColorConst(ColorConst colorConst) {
        this.src.add("(", colorConst.getRedChannelInt(), ", ", colorConst.getGreenChannelInt(), ", ", colorConst.getBlueChannelInt(), ")");
        return null;
    }

    @Override
    public Void visitRgbColor(RgbColor rgbColor) {
        this.src.add("(");
        rgbColor.R.accept(this);
        this.src.add(", ");
        rgbColor.G.accept(this);
        this.src.add(", ");
        rgbColor.B.accept(this);
        this.src.add(")");
        return null;
    }
}