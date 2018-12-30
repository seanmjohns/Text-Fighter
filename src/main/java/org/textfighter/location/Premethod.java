package org.textfighter.location;

import org.textfighter.location.Location;
import org.textfighter.display.Display;
import org.textfighter.Requirement;

import java.util.ArrayList;

import java.lang.reflect.*;

@SuppressWarnings("unchecked")

public class Premethod {

    private Location location;
    private ArrayList<Object> arguments;
    private Class clazz;
    private Method method;
    private Field field;
    private ArrayList<Requirement> requirements = new ArrayList<Requirement>();

    public ArrayList<Object> getArguments() { return arguments; }
    public Class getClazz() { return clazz; }
    public Method getMethod() { return method; }
    public ArrayList<Requirement> getRequirements() { return requirements; }

    public void invokeMethod() {
        try {
            if(field != null ) {
                method.invoke(field, arguments);
            } else {
                method.invoke(arguments);
            }
        } catch (IllegalAccessException | InvocationTargetException e) { e.printStackTrace(); }
    }

    public Premethod(String method, ArrayList<String> arguments, ArrayList<Class> argumentTypes, String clazz, String field, ArrayList<Requirement> requirements) {
        try { this.clazz = Class.forName(clazz); } catch (ClassNotFoundException e){ e.printStackTrace(); System.exit(1); }
        try {
            if(field != null && !field.isEmpty()) {
                this.field = this.clazz.getField(field);
            }
        } catch (NoSuchFieldException | SecurityException e) { e.printStackTrace(); System.exit(1);}
        if(argumentTypes != null ) {
            try { this.method = this.clazz.getMethod(method, argumentTypes.toArray(new Class[argumentTypes.size()])); } catch (NoSuchMethodException e){ e.printStackTrace(); System.exit(1); }
        } else {
            try { this.method = this.clazz.getMethod(method); } catch (NoSuchMethodException e){ e.printStackTrace(); System.exit(1); }
        }

        if(this.method.getParameterTypes().length != argumentTypes.size()) { Display.displayWarning("There is an incorrect number of arguments for the location '" + location.getName() + "' premethod '" + method + "' parameters! Needed: " + this.method.getParameterTypes().length + " Got: " + argumentTypes.size()); }
        if(arguments != null) {
            Class[] parameterTypes = this.method.getParameterTypes();
            for (int i=0; i<arguments.size(); i++) {
                if(parameterTypes[i].equals(Integer.class)) {
                    this.arguments.add(Integer.parseInt((String)arguments.get(i)));
                }
            }
        }
        this.requirements = requirements;
    }

}