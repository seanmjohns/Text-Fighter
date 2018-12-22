package org.textfighter.location;

import java.lang.reflect.*;

import java.util.ArrayList;

@SuppressWarnings("unchecked")

public class ChoiceMethod {

    private Method method;
    private Class clazz;
    private Field field;
    private ArrayList<Object> originalArguments;
    private ArrayList<Object> arguments;
    private ArrayList<Class> argumentTypes;

    public Method getMethod() { return method; }
    public Class getClazz() { return clazz; }
    public Field getField() { return field; }
    public ArrayList<Object> getArguments() { return arguments; }
    public void setArguments(ArrayList<Object> args) { arguments=args; }
    public ArrayList<Class> getArgumentTypes() { return argumentTypes; }

    public boolean invokeMethod() {
        if(arguments.size() != method.getParameterTypes().length) {
            return false;
        } else {
            try {
                method.invoke(field, arguments.toArray());
                arguments = originalArguments;
                return true;
            } catch (IllegalAccessException | InvocationTargetException e) { e.printStackTrace(); }
        }
        arguments = originalArguments;
        return false;
    }

    public ChoiceMethod(String method, ArrayList<String> arguments, ArrayList<Class> argumentTypes, String clazz, String field) {

        this.argumentTypes = argumentTypes;
        //Creates the method
        try {
            this.clazz = Class.forName(clazz);
        } catch (ClassNotFoundException e){
            e.printStackTrace();
            System.exit(1);
        }
        try {
            if(field != null) {
                this.field = this.clazz.getField(field);
            }
        } catch (NoSuchFieldException | SecurityException e) { e.printStackTrace(); System.exit(1);}
        try {
            if(argumentTypes != null) {
                this.method = this.clazz.getMethod(method, argumentTypes.toArray(new Class[argumentTypes.size()]));
            } else {
                this.method = this.clazz.getMethod(method);
            }
        } catch (NoSuchMethodException e){
            e.printStackTrace();
            System.exit(1);
        }
        for (int i=0; i<arguments.size(); i++) {
            if(this.method.getParameterTypes()[i].equals(Integer.class)) {
                this.arguments.add(Integer.parseInt(arguments.get(i)));
            }
        }
        this.originalArguments = this.arguments;
    }
}
