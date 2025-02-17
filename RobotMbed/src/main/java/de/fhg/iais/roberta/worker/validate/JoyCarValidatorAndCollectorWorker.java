package de.fhg.iais.roberta.worker.validate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ClassToInstanceMap;

import de.fhg.iais.roberta.bean.IProjectBean;
import de.fhg.iais.roberta.components.Project;
import de.fhg.iais.roberta.util.syntax.SC;
import de.fhg.iais.roberta.visitor.JoycarMethods;
import de.fhg.iais.roberta.visitor.validate.CommonNepoValidatorAndCollectorVisitor;
import de.fhg.iais.roberta.visitor.validate.JoyCarValidatorAndCollectorVisitor;

public class JoyCarValidatorAndCollectorWorker extends MbedValidatorAndCollectorWorker {
    public static final List<String> FREE_PINS = Stream.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "19", "20").collect(Collectors.toList());
    public static final List<String> DEFAULT_PROPERTIES = Stream.of("KEY", "ACCELEROMETER", "COMPASS", "TEMPERATURE", "LIGHT", "ROBOT", "SOUND", "BUZZER", "LOGOTOUCH", "SERVOMOTOR", "ULTRASONIC", "RGBLED", "MOTOR", "DIFFERENTIALDRIVE", "ENCODER", "I2C_BUS", "INFRARED", "LINE").collect(Collectors.toList());
    public static final HashMap<String, String> MAP_CORRECT_CONFIG_PINS = new HashMap<String, String>() {{
        put(SC.BUZZER, "0");
    }};

    @Override
    protected List<Class<? extends Enum<?>>> getAdditionalMethodEnums() {
        return Collections.singletonList(JoycarMethods.class);
    }

    public JoyCarValidatorAndCollectorWorker() {
        super(FREE_PINS, DEFAULT_PROPERTIES, MAP_CORRECT_CONFIG_PINS);
    }

    @Override
    protected CommonNepoValidatorAndCollectorVisitor getVisitor(Project project, ClassToInstanceMap<IProjectBean.IBuilder> beanBuilders) {
        return new JoyCarValidatorAndCollectorVisitor(project.getConfigurationAst(), beanBuilders, false, isDisplaySwitchUsed(project));
    }
}
