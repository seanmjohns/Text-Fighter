package org.textfighter;

import java.util.ArrayList;
import java.io.*;

import org.textfighter.Display;
import org.textfighter.item.*;
import org.textfighter.item.armor.*;
import org.textfighter.item.weapon.*;
import org.textfighter.Player;
import org.textfighter.userinterface.*;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TextFighter {

    static String gameName;

    static Player player;

    static File resourcesDir;
    static File tagFile;
    static File interfaceDir;
    static File enemyDir;
    static File savesDir;

    static JSONParser parser = new JSONParser();

    static ArrayList<UserInterface> userInterfaces = new ArrayList<UserInterface>();
    static ArrayList<UiTag> interfaceTags = new ArrayList<UiTag>();

    public static boolean loadResources() {
        resourcesDir = new File("../res");
        tagFile = new File(resourcesDir + "/tags/tags.json");
        interfaceDir = new File(resourcesDir + "/interfaces");
        savesDir = new File("../../../saves");
        if (!loadInterfaces() || !loadParsingTags()) { return false; }
        if (!savesDir.exists()) {
            System.out.println("WARNING: The save directory does not exist!\nCreating a new saves directory...");
            if (!new File("../../../saves/").mkdirs()) { System.out.println("Unable to create a new saves directory!"); return false; }
        }
        return true;
    }

    public static boolean loadInterfaces() {
        try {
            for (String f : interfaceDir.list()) {
                if(!f.substring(f.indexOf(".")).equals(".json")) { continue; }
                JSONObject uiArrayFile = (JSONObject) parser.parse(new FileReader(new File(interfaceDir.getAbsolutePath() + "/" + f)));
                JSONArray uiArray = (JSONArray)uiArrayFile.get("Interface");
                String name = (String) uiArrayFile.get("name");
                String uiString = "";
                for (int u = 0; u < uiArray.size(); u++) { uiString = uiString + uiArray.get(u) + "\n"; }
                userInterfaces.add(new UserInterface(name, uiString));
                //TODO Load the choices
            }
        } catch (IOException | ParseException e) { e.printStackTrace(); return false; }
        return true;
    }

    public static boolean loadParsingTags() {
        try {
            JSONObject tagsFile = (JSONObject)parser.parse(new FileReader(tagFile));
            JSONArray tagsArray = (JSONArray)tagsFile.get("tags");
            for (int i = 0; i < tagsArray.size(); i++) {
                JSONObject object = (JSONObject)tagsArray.get(i);
                interfaceTags.add(new UiTag((String)object.get("tag"),(String)object.get("function")));
            }
        } catch (IOException | ParseException e) { e.printStackTrace(); return false; }
        return true;

    }

    public static boolean loadSave(String name) {
        //Read from /Saves directory


        return true;

    }

    public static void newGame(String name) {
        //Creates the file that the save is in
        File newGameFile = new File(savesDir.getPath() + "/" + name + ".json");
        try { newGameFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }

        //Write the basic things to the file
        JSONObject base = new JSONObject();

        JSONObject stats = new JSONObject();
        stats.put("level", "1");
        stats.put("experience", "0");
        stats.put("score", "0");

        JSONObject inventory = new JSONObject();
        inventory.put("sword", "true");
        inventory.put("bow", "false");
        inventory.put("pickaxe", "false");
        inventory.put("helmet", "false");
        inventory.put("chestplate", "false");
        inventory.put("leggings", "false");
        inventory.put("boots", "false");
        inventory.put("coins", "0");

        JSONObject sword = new JSONObject();
        sword.put("type", "0");
        sword.put("level", "1");
        sword.put("experience", "0");
        inventory.put("sword", sword);

        base.put("inventory", inventory);
        base.put("stats", stats);
        base.put("name", name);

        try (FileWriter w = new FileWriter(newGameFile);) {
            w.write(base.toJSONString());
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static boolean saveGame() {
        //Rewrite the whole file

        File gameFile = new File(savesDir.getPath() + "/" + gameName + ".json");
        if(!gameFile.exists()) { try { gameFile.createNewFile(); } catch (IOException e) { System.out.println("Unable to save game!"); return false; }}

        JSONObject base = new JSONObject();

        JSONObject stats = new JSONObject();
        stats.put("level", player.getLevel());
        stats.put("experience", player.getExperience());
        stats.put("score", player.getScore());

        JSONObject inventory = new JSONObject();

        //Puts these things if the player has them
        if(player.isCarrying("Sword")>0) { inventory.put("sword", "true"); } else { inventory.put("sword", "false"); }
        if(player.isCarrying("Bow")>0) { inventory.put("bow", "true"); } else { inventory.put("bow", "false"); }
        if(player.isCarrying("Pickaxe")>0) { inventory.put("pickaxe", "true"); } else { inventory.put("pickaxe", "false");}
        if(player.isCarrying("Helmet")>0) { inventory.put("helmet", "true"); } else {inventory.put("helmet", "false");}
        if(player.isCarrying("Chestplate")>0) { inventory.put("chestplate", "true"); } else {inventory.put("chestplate", "false");}
        if(player.isCarrying("Leggings")>0) { inventory.put("leggings", "true"); } else {inventory.put("leggings", "false");}
        if(player.isCarrying("Boots")>0) { inventory.put("boots", "true"); } else { inventory.put("boots", "false"); }

        inventory.put("coins", "0");

        JSONObject sword = new JSONObject();
        sword.put("type", "0");
        sword.put("level", "1");
        sword.put("experience", "0");
        inventory.put("sword", sword);

        base.put("inventory", inventory);
        base.put("stats", stats);
        base.put("name", gameName);

        return true;

    }

    public static void fight() {

    }

    public static ArrayList<String> getSaveFiles() {
        ArrayList<String> filteredSaves = new ArrayList<String>();
        for(String s : savesDir.list()) {
            if(s.substring(s.indexOf(".")).equals(".json")) {
                filteredSaves.add(s);
            }
        }
        return filteredSaves;
    }

    public static void main(String[] args) {
        //System.out.println("\u001b[2J");
        if (!loadResources()) {
            System.out.println("An error occured while trying to load the resources!\nMake sure they are in the correct directory.");
            System.exit(0);
        }
        newGame("ree");
        // Display all saves
        // Ask if user want to load saves
        // if yes, then load
        // else, make new
    }

}
