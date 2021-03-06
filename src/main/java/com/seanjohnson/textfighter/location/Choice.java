package com.seanjohnson.textfighter.location;

import com.seanjohnson.textfighter.method.*;

import com.seanjohnson.textfighter.TextFighter;

import java.lang.reflect.*;

import java.util.*;

public class Choice {

    /***Stores the name of this choice.*/
    private String name;
    /***Stores the description of this choice.*/
    private String description;
    /***Stores the usage of this choice.*/
    private String usage;
    /***Stores the message that is outputted when the {@link #methods} do not meet their own requirements.*/
    private String failMessage;

    /**
     * Stores the methods of this choice.
     * <p>Set to an empty ArrayList of ChoiceMethods.</p>
     */
    private ArrayList<ChoiceMethod> methods = new ArrayList<ChoiceMethod>();
    /**
     * Stores the requirements of this choice.
     * <p>Set to an empty ArrayList of Requirements.</p>
     */
    private ArrayList<Requirement> requirements = new ArrayList<Requirement>();

    /**
     * Returns the {@link #name}.
     * @return      {@link #name}
     */
    public String getName() { return name; }
    /**
     * Returns the {@link #usage}.
     * @return      {@link #usage}
     */
    public String getDescription() { return description; }
    /**
     * Returns the {@link #usage}.
     * @return      {@link #usage}
     */
    public String getUsage() { return usage; }
    /**
     * Returns the {@link #failMessage}.
     * @return      {@link #failMessage}
     */
    public String getFailtMessage() { return failMessage; }
    /**
     * Returns the {@link #methods}.
     * @return      {@link #methods}
     */
    public ArrayList<ChoiceMethod> getMethods() { return methods; }
    /**
     * Returns the {@link #requirements}.
     * @return      {@link #requirements}
     */
    public ArrayList<Requirement> getRequirements() { return requirements; }

    /**
     * Returns the name, description, and usage of the choice.
     * @return      name, description, and usage
     */
    public String getOutput() {
        return name + "\n" +
            "\tdescription  - " + description + "\n" +
            "\tusage - " + usage;
    }

    /**
     * Puts the arguments in the {@link #methods} and invokes them.
     * <p>First loops through the {@link #methods} and puts arguments in placeholders and then invokes them.
     * If there is a problem with invoking the methods, then say so and print out the usage.</p>
     * @param inputArgs     The player's input arguments.
     * @return              Returns whether or not successful.
     */
    public boolean invokeMethods(ArrayList<String> inputArgs) {
        for(ChoiceMethod cm : methods) {
            //Put the input in the arguments of all the ChoiceMethod
            if(!cm.putInputInArguments(inputArgs)) {
                for(ChoiceMethod m : methods) {
                    m.resetArguments();
                }
                TextFighter.addToOutput("Problem with parsing input.\nUsage: " + usage);
                return false;
            }
        }
        for(ChoiceMethod m : methods) {
            boolean valid = true;
            if(m.getRequirements() != null) {
                for(Requirement r : m.getRequirements()) {
                    if(!r.invokeMethod()) {
                        valid = false;
                        break;
                    }
                }
            }
            if(valid) {
                if(!m.invokeMethod()) {
                    TextFighter.addToOutput("Problem with invoking method with given arguments.\nUsage: " + usage);
                    return false;
                }
            } else {
                if(failMessage != null) {
                    TextFighter.addToOutput(failMessage);
                    return true;
                }
            }
            m.resetArguments();
        }
        return true;
    }

    public Choice(String name, String description, String usage, ArrayList<ChoiceMethod> methods, ArrayList<Requirement> requirements, String failMessage) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.methods = methods;
        this.requirements = requirements;
        this.failMessage = failMessage;
    }
}
