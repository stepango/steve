package com.stepango.steve;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class JobUtils {

    public static final String LOG_FORMAT = "%s:: %s";
    public static String TAG = JobUtils.class.getName();

    private JobUtils() {
        throw new UnsupportedOperationException();
    }

    public static JobParams extractJobParams(Job job) {
        JobParams params = new JobParams();
        List<Field> declaredFields = fields(job);
        for (Field field : declaredFields) {
            if (!containsAnnotation(field.getAnnotations(), Serialize.class)) {
                continue;
            }
            try {
                Object value = field.get(job);
                if (value != null) {
                    params.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                log(TAG, e);
            }
        }
        return params;
    }

    private static boolean containsAnnotation(Annotation[] array, Class obj) {
        for (Annotation item : array) {
            if (Objects.equals(item.annotationType(), obj)) {
                return true;
            }
        }
        return false;
    }

    public static void log(String tag, Throwable e) {
        log(tag, e.getMessage());
        e.printStackTrace();
    }

    private static void log(String tag, String msg) {
        System.out.println(String.format(LOG_FORMAT, tag, msg));
    }

    public static List<Field> fields(Object obj) {
        List<Field> fieldList = new ArrayList<>();
        Class tmpClass = obj.getClass();
        while (tmpClass != null) {
            fieldList.addAll(Arrays.asList(tmpClass.getDeclaredFields()));
            tmpClass = tmpClass.getSuperclass();
        }
        return fieldList;
    }
}
