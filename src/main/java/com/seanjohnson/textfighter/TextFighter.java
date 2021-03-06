package com.seanjohnson.textfighter;

/*

MIT License

Copyright (c) 2019 Sean Johnson

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/

import java.nio.file.*;
import java.util.*;
import java.io.*;

import java.lang.reflect.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.seanjohnson.textfighter.display.*;
import com.seanjohnson.textfighter.item.*;
import com.seanjohnson.textfighter.location.*;
import com.seanjohnson.textfighter.enemy.*;
import com.seanjohnson.textfighter.method.*;

import org.json.simple.*;
import org.json.simple.parser.*;
import org.json.simple.parser.ParseException;

/* HOW IT WORKS

    Mods are loaded.
Methods are extracted from the mods.
Methods are specified in the method JSONObjects and their
classes are specified with them.
The methods are extracted from the classes specified.
The pack also specifies arguments and classes of the arguments.

    Methods are invoked to do different things:
Determine if another method should be invoked (Among other things),
Just do regular things like set a value,
show output to the user,
make an enemy perform an action,
And a lot more.

    ChoiceMethods have arguments that are inputted by the user.
These arguments are specified by "%ph%[inputIndex]", and are replaced
just before they are invoked. If an argument is a method,
then placeholder arguments are replaced by input.
    If something went wrong with putting input into the arguments,
the user is notified and the usage of the choices is outputted.

*/

@SuppressWarnings("unchecked")

public class TextFighter {

    /**Object that waits for a message*/
    public static Object waiter = new Object();

    /**The default installation location on Windows10/8/7*/
    public static File windowsInstallLocation; //we cannot run System.getenv("APPDATA") on OSes other than windows

    /**The default installation location on MacOS*/
    public static File macInstallLocation = new File(System.getProperty("user.home") + "/Library/Application support/");

    /**The default installation location on other OSes (like Linux, and literally any other OS to ever exist)*/
    public static File otherInstallLocation = new File(System.getProperty("user.home") + "/");

    /**Application installation directory name*/
    public static String appInstallDirName = "textfighter";

    /**Stores the install location*/
    public static File installationRoot;

    /**
     * Installs the game.
     * Installation locations:
     *      Mac: ~/Library/Application Support/textfighter
     *      Windows: C:\\ProgramFiles\textfighter
     *      Linux: /opt/textfighter
     *      Other operating systems are not supported.
     * @return  True if successful, false if not.
     */
    public static boolean installGame() {

        String operatingSystem = System.getProperty("os.name");

        File installationLocation;

        boolean windowsOrMac = true;

        //Get the place the game will be installed (depends on OS)
        if(operatingSystem.contains("Windows")) { //Windows
            installationLocation = new File(System.getenv("APPDATA"));
        } else if (operatingSystem.contains("Mac") || operatingSystem.contains("OS X")) { //Macos
            installationLocation = macInstallLocation;
        } else { //Other (including linux)
            windowsOrMac = false;
            installationLocation = otherInstallLocation;
        }

        //Install the game in the location
        if(installationLocation.exists()) {
            //Copy files and create directories there
            installationRoot = new File(installationLocation.getAbsolutePath() + File.separator + appInstallDirName);
            if(!windowsOrMac) { installationRoot = new File(installationLocation.getAbsolutePath() + File.separator + "." + appInstallDirName); } //Make it a dotfile instead cuz linux or something

            if(!installationRoot.exists()) {
                Display.displayProgressMessage("Installing TextFighter");
                Display.displayProgressMessage("Creating Directory: " + installationRoot.getAbsolutePath());
                if(!installationRoot.mkdir()) { return false; } //NOTE: Do NOT create parent directories if they were deleted just after the check. Just let the installation fail.
            }

            boolean updatedSinceLastLaunch = false;

            //Version File
            installationVersionFile = new File(installationRoot.getAbsoluteFile() + File.separator + versionFile);
            if(!installationVersionFile.exists() || !version.equalsIgnoreCase(readVersionFromInstallationLocation())) { //We will need to copy the guide and the newest version over
                updatedSinceLastLaunch = true;
                copyFileFromJar(new File("/VERSION.txt"), installationVersionFile);
            }

            //Saves
            savesDir = new File(installationRoot.getAbsolutePath() + File.separator + "saves");
            if(!savesDir.exists()) {
                Display.displayProgressMessage("Creating Directory: " + savesDir.getAbsolutePath());
                if(!savesDir.mkdirs()) { return false; }
            }

            //Packs
            packDir = new File(installationRoot.getAbsolutePath() + File.separator + "packs");
            if(!packDir.exists()) {
                Display.displayProgressMessage("Creating Directory: " + packDir.getAbsolutePath());
                if(!packDir.mkdirs()) { return false; }
            }

            //Config
            configDir = new File(installationRoot.getAbsolutePath() + File.separator + "config");
            if(!configDir.exists()) {
                Display.displayProgressMessage("Creating Directory: " + configDir.getAbsolutePath());
                if(!configDir.mkdirs()) { return false; }
            }

                //I hate it when people do this, but it feels a little more organized this way.
                packFile = new File(configDir.getAbsolutePath() + File.separator + "pack.txt");
                if(!packFile.exists()) {
                    Display.displayProgressMessage("Creating File: " + packFile.getAbsolutePath());
                    try { if(!packFile.createNewFile()) { return false; } } catch (IOException e) { e.printStackTrace(); Display.displayWarning("Unable to create the modpack configuration file."); }
                }

                File displayFile = new File(configDir.getAbsolutePath() + File.separator + "display.txt");
                if(!displayFile.exists()) {
                    Display.displayProgressMessage("Creating File: " + displayFile.getAbsolutePath());
                    try { if(!displayFile.createNewFile()) { return false; } } catch (IOException e) { e.printStackTrace(); Display.displayWarning("Unable to create the display colors configuration file."); }
                }

            //logging
            Display.logDir = new File(installationRoot.getAbsolutePath() + File.separator + "logs");
            if(!Display.logDir.exists()) {
                Display.displayProgressMessage("Creating Directory: " + Display.logDir.getAbsolutePath());
                if(!Display.logDir.mkdirs()) { return false; }
            }

            //The vanilla guide
            vanillaGuideDir = new File(installationRoot.getAbsolutePath() + File.separator + "guide");
            if(updatedSinceLastLaunch) { //The guide should only be copied when a new version is released
                //Clear out the entire vanilla guide directory
                clearDirectory(vanillaGuideDir);
                //Create (or recreate) the vanilla guide directory and copy all the stuff into it from the jar.
                if(!copyVanillaGuide()) {
                    Display.displayError("Unable to copy the vanilla guide to the installation directory.");
                }
            } if (!vanillaGuideDir.exists()) {
                if(!copyVanillaGuide()) {
                    Display.displayError("Unable to copy the vanilla guide to the installation directory.");
                }
            }

        } else {
            Display.displayError("Could not find directory '" + installationLocation.getAbsolutePath() + "' for installation");
            return false;
        }
        return true;

    }

    // Pack testing variables
    /** True if testing packs.*/
    public static boolean testMode = false;
    /**Stores whether or not we should be compiling a mod guide right now*/
    public static boolean guideCompileMode = false;
    /**True if loading assets from a pack.*/
    public static boolean parsingPack = false;

    /** True if running from jar */
    public static boolean runFromJar = false;

    //Version
    /**The file that the version of the game is stored in.*/
    public static String versionFile = "VERSION.txt";
    /**Stores the current version.*/
    public static String version;

    // Important game variables
    /**Stores the name of the save currently being used.*/
    public static String gameName;

    /**Stores the player instance.*/
    public static Player player;
    /**Stores the enemy instance that the player is fighting (Assuming the player is in a fight).*/
    public static Enemy currentEnemy = new Enemy();

    /**Stores the file that the save is stored in*/
    public static File currentSaveFile;

    /**Stores the String that is outputted after every turn*/
    public static String output = "";

    /***Stores the name of the mod being used.*/
    public static String modName;

    /***Stores whether or not the player has acted since the fight has started*/
    public static boolean actedSinceStartOfFight = false;

    /**Whether or not {@link #loadConfig()} has been called*/
    public static boolean configLoaded = false;

    /**
     * Stores the directory of the used pack.
     * If there is no pack, this will remain null.
     */
    public static File packUsed;
    /**Stores the directory where all of the default assets are located*/
    public static File assetsDir;
    /**Stores the file where the default tags are located*/
    public static File tagFile;
    /**Stores the directory where the default pack default values are located*/
    public static File defaultValuesDirectory;
    /**Stores the directory where the default interfaces are located*/
    public static File interfaceDir;
    /**Stores the directory where the default locations are located*/
    public static File locationDir;
    /**Stores the directory where the default enemies are located*/
    public static File enemyDir;
    /**Stores the directory where the saves are located*/
    public static File savesDir;
    /**Stores the directory where the default achievements are located*/
    public static File achievementDir;
    /**Stores the directory where the items are located*/
    public static File itemDir;
    /**Stores the directory where the default custom variables are located*/
    public static File customVariablesDir;
    /**Stores the directory where the death methods are located*/
    public static File deathmethodsFile;
    /**Stores the directory where the level up methods are located*/
    public static File levelupmethodsFile;
    /**Stores the directory where the choices of all locations are located*/
    public static File choicesOfAllLocationsFile;

    /**Stores the directory where all config files are located*/
    public static File configDir;

    /**Stores the path to the version file in the installation directory*/
    public static File installationVersionFile;

    /**Stores the location of the vanilla textfighter guide*.
     *
     */
    public static File vanillaGuideDir;

    /**Stores the file where the pack used is defined*/
    public static File packFile;
    /**Stores the directory where all packs are to be put*/
    public static File packDir;

    /**The parser for all assets and saves*/
    public static JSONParser parser = new JSONParser();

    //All things in the game
    /**All locations are stored here*/
    public static ArrayList<Location> locations = new ArrayList<>();
    /**All save names are stored here*/
    public static ArrayList<String> saves = new ArrayList<>();
    /**All enemies are stored here*/
    public static ArrayList<Enemy> enemies = new ArrayList<>();
    /**All possible enemies are stored here*/
    public static ArrayList<Enemy> possibleEnemies = new ArrayList<>();
    /**All achievements are stored here*/
    public static ArrayList<Achievement> achievements = new ArrayList<>();
    /**All armors are stored here*/
    public static ArrayList<Armor> armors = new ArrayList<>();
    /**All weapons are stored here*/
    public static ArrayList<Weapon> weapons = new ArrayList<>();
    /**All specialitems are stored here*/
    public static ArrayList<SpecialItem> specialItems = new ArrayList<>();
    /**All tools are stored here*/
    public static ArrayList<Tool> tools = new ArrayList<>();

    //All custom variable arrays
    /**All player custom variables are copied from here to the new instance*/
    public static ArrayList<CustomVariable> playerCustomVariables = new ArrayList<>();
    /**All enemy custom variables are copied from here to the new instance*/
    public static ArrayList<CustomVariable> enemyCustomVariables = new ArrayList<>();
    /**All tool custom variables are copied from here to the new instance*/
    public static ArrayList<CustomVariable> toolCustomVariables = new ArrayList<>();
    /**All armor custom variables are copied from here to the new instance*/
    public static ArrayList<CustomVariable> armorCustomVariables = new ArrayList<>();
    /**All weapon custom variables are copied from here to the new instance*/
    public static ArrayList<CustomVariable> weaponCustomVariables = new ArrayList<>();
    /**All special item custom variables are copied from here to the new instance*/
    public static ArrayList<CustomVariable> specialitemCustomVariables = new ArrayList<>();

    /**All the death methods are copied from here to the new instance of player*/
    public static ArrayList<IDMethod> deathMethods = new ArrayList<>();
    /**All the level up methods are copied from here to the new instance of player*/
    public static ArrayList<IDMethod> levelupMethods = new ArrayList<>();
    /**All the choices of all locations are copied from here to the loaded location*/
    public static ArrayList<ChoiceOfAllLocations> choicesOfAllLocations = new ArrayList<>();

    /**The recent input commands saved. This is only used in the gui.*/
    public static HistoryLinkedList<String> inputHistory = new HistoryLinkedList<>();

    public static ArrayList<String> getNamesFromJarFile(String path) {
        ArrayList<String> names = new ArrayList<>();
        try {
            final File jarFile = new File(TextFighter.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            if(jarFile.isFile()) {  // Run with JAR file
                final JarFile jar = new JarFile(jarFile);
                final Enumeration<JarEntry> entries = jar.entries();
                while(entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.startsWith(path.substring(1) + "/")) { //filter according to the path and json file
                        if(name.endsWith("/")) { //It is a directory
                            names.add(name.substring(name.lastIndexOf("/", name.length()-2)));
                        } else {
                            names.add(name.substring(name.lastIndexOf('/')));
                        }
                    }
                }
                jar.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return names;
    }

    /**
     * Copies vanilla textfighter guide files missing from the mod guide.
     */
    public static void compileGuide() {
        if (packUsed == null) {
            Display.displayError("Not using a pack, cannot compile a guide for it.");
            return;
        }

        File guideDirectory = new File(packUsed + File.separator + "guide");
        if (!guideDirectory.exists() || !guideDirectory.isDirectory()) {
            Display.displayError("This pack has no guide directory within it, so nothing to compile.");
            return;
        }

        if(runFromJar) { //then the game is run from a jar
            //Locations

            File directory = getPackDirectory("locations", packUsed);
            ArrayList<String> namesToBeOmitted = new ArrayList<>();
            if (directory != null) {
                namesToBeOmitted = getOmittedAssets(new File(directory + File.separator + "omit.txt"));
            }
            if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                for (String s : getNamesFromJarFile(locationDir.getPath())) {
                    if (!s.contains(".json")) {
                        continue;
                    } //The method grabbed a directory here.
                    if (!namesToBeOmitted.contains(s)) {
                        File destDir = new File(guideDirectory.getAbsoluteFile() + File.separator + "locations");
                        destDir.mkdirs(); //Will fail silently if it already exists
                        String newName = s.substring(0,s.length()-5);
                        File destFile = new File(destDir + File.separator + newName);
                        if (!destFile.exists()) {
                            File guideFile = new File("/guide/locations/" + s);
                            copyFileFromJar(guideFile, destFile);
                        } //else: it already exists, so dont touch it
                    }
                }
            }

            //items
            directory = getPackDirectory("items", packUsed);
            for (String type : getNamesFromJarFile(itemDir.getPath())) {
                if (type.contains(".txt") || type.contains(".json")) {
                    continue;
                } //Ignore it. This is not a directory.
                if (type.equalsIgnoreCase("/weapons/")) {
                    File weaponsDir = new File(directory + "/weapons");
                    File jarDir = new File(itemDir.getPath() + "/weapons");
                    namesToBeOmitted = new ArrayList<>();
                    if (directory != null) {
                        namesToBeOmitted = getOmittedAssets(new File(weaponsDir + File.separator + "omit.txt"));
                    }
                    if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                        for (String s : getNamesFromJarFile(jarDir.getPath())) {
                            if (s.endsWith("/")) {
                                continue;
                            } //It is a directory so ignore it
                            if (!namesToBeOmitted.contains(s)) {
                                File destDir = new File(guideDirectory.getAbsoluteFile() + File.separator + "items" + File.separator + "weapons");
                                destDir.mkdirs(); //Will fail silently if it already exists so we vibin
                                String newName = s.substring(0,s.length()-5);
                                File destFile = new File(destDir + File.separator + newName);
                                if (!destFile.exists()) {
                                    copyFileFromJar(new File("/guide/items/weapons/" + s), destFile);
                                } //else: it already exists, so dont touch it
                            }
                        }
                    }
                } else if (type.equalsIgnoreCase("/tools/")) {
                    File toolsDir = new File(directory + "/tools");
                    File jarDir = new File(itemDir.getPath() + "/tools");
                    namesToBeOmitted = new ArrayList<>();
                    if (directory != null) {
                        namesToBeOmitted = getOmittedAssets(new File(toolsDir + File.separator + "omit.txt"));
                    }
                    if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                        for (String s : getNamesFromJarFile(jarDir.getPath())) {
                            if (s.endsWith("/")) {
                                continue;
                            } //It is a directory so ignore it
                            if (!namesToBeOmitted.contains(s)) {
                                File destDir = new File(guideDirectory.getAbsoluteFile() + File.separator + "items" + File.separator + "tools");
                                destDir.mkdirs(); //Will fail silently if it already exists so we vibin
                                String newName = s.substring(0,s.length()-5);
                                File destFile = new File(destDir + File.separator + newName);
                                if (!destFile.exists()) {
                                    copyFileFromJar(new File("/guide/items/tools/" + s), destFile);
                                } //else: it already exists, so dont touch it
                            }
                        }
                    }
                } else if (type.equalsIgnoreCase("/armor/")) {
                    File armorDir = new File(directory + "/armor");
                    File jarDir = new File(itemDir.getPath() + "/armor/");
                    namesToBeOmitted = new ArrayList<>();
                    if (directory != null) {
                        namesToBeOmitted = getOmittedAssets(new File(armorDir + File.separator + "omit.txt"));
                    }
                    if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                        for (String s : getNamesFromJarFile(jarDir.getPath())) {
                            if (s.endsWith("/")) {
                                continue;
                            } //It is a directory so ignore it
                            if (!namesToBeOmitted.contains(s)) {
                                File destDir = new File(guideDirectory.getAbsoluteFile() + File.separator + "items" + File.separator + "armor");
                                destDir.mkdirs(); //Will fail silently if it already exists so we vibin
                                String newName = s.substring(0,s.length()-5);
                                File destFile = new File(destDir + File.separator + newName);
                                if (!destFile.exists()) {
                                    copyFileFromJar(new File("/guide/items/armor/" + s), destFile);
                                } //else: it already exists, so dont touch it
                            }
                        }
                    }
                } else if (type.equalsIgnoreCase("/specialitems/")) {
                    File specialitemsDir = new File(directory + "/specialitems");
                    File jarDir = new File(itemDir.getPath() + "/specialitems");
                    namesToBeOmitted = new ArrayList<>();
                    if (directory != null) {
                        namesToBeOmitted = getOmittedAssets(new File(specialitemsDir + File.separator + "omit.txt"));
                    }
                    if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                        for (String s : getNamesFromJarFile(jarDir.getPath())) {
                            if (s.endsWith("/")) {
                                continue;
                            } //It is a directory so ignore it
                            if (!namesToBeOmitted.contains(s)) {
                                File destDir = new File(guideDirectory.getAbsolutePath() + File.separator + "items" + File.separator + "specialitems");
                                destDir.mkdirs(); //Will fail silently if it already exists so we vibin
                                String newName = s.substring(0,s.length()-5);
                                File destFile = new File(destDir + File.separator + newName);
                                if (!destFile.exists()) {
                                    copyFileFromJar(new File("/guide/items/specialitems/" + s), destFile);
                                } //else: it already exists, so dont touch it
                            }
                        }
                    }
                }
            }

            //Enemies
            directory = getPackDirectory("enemies", packUsed);
            namesToBeOmitted = new ArrayList<>();
            if (directory != null) {
                namesToBeOmitted = getOmittedAssets(new File(directory + File.separator + "omit.txt"));
            }
            if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                for (String s : getNamesFromJarFile(enemyDir.getPath())) {
                    if (!s.contains(".json")) {
                        continue;
                    } //The method grabbed a directory here.
                    if (!namesToBeOmitted.contains(s)) {
                        File destDir = new File(guideDirectory.getAbsolutePath() + File.separator + "enemies");
                        destDir.mkdirs(); //Will fail silently if it already exists
                        String newName = s.substring(0,s.length()-5);
                        File destFile = new File(destDir + File.separator + newName);
                        if (!destFile.exists()) {
                            copyFileFromJar(new File("/guide/enemies/" + s), destFile);
                        } //else: it already exists, so dont touch it
                    }
                }
            }

            //Achievements
            directory = getPackDirectory("achievements", packUsed);
            namesToBeOmitted = new ArrayList<>();
            if (directory != null) {
                namesToBeOmitted = getOmittedAssets(new File(directory + File.separator + "omit.txt"));
            }
            if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                for (String s : getNamesFromJarFile(achievementDir.getPath())) {
                    if (!s.contains(".json")) {
                        continue;
                    } //The method grabbed a directory here.
                    if (!namesToBeOmitted.contains(s)) {
                        File destDir = new File(guideDirectory.getAbsolutePath() + File.separator + "achievements");
                        destDir.mkdirs(); //Will fail silently if it already exists
                        String newName = s.substring(0,s.length()-5);
                        File destFile = new File(destDir + File.separator + newName);
                        if (!destFile.exists()) {
                            copyFileFromJar(new File("/guide/achievements/" + s), destFile);
                        } //else: it already exists, so dont touch it
                    }
                }
            }

            /*For the following, we must figure out if they exist first. If not, then copy them.*/
            //death

            File file = getPackFile("death", guideDirectory);
            if (file == null) {
                copyFileFromJar(new File("/guide/death.txt"), new File(guideDirectory + "/death"));
            }

            //player

            file = getPackFile("player", guideDirectory);
            if (file == null) {
                copyFileFromJar(new File("/guide/player.txt"), new File(guideDirectory + "/player"));
            }

            //levelup

            file = getPackFile("levelup", guideDirectory);
            if (file == null) {
                copyFileFromJar(new File("/guide/levelup.txt"), new File(guideDirectory + "/levelup"));
            }
        } else { ///running from ide

            //Locations

            File directory = getPackDirectory("locations", packUsed);
            ArrayList<String> namesToBeOmitted = new ArrayList<>();
            if (directory != null) {
                namesToBeOmitted = getOmittedAssets(new File(directory + File.separator + "omit.txt"));
            }
            if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                for (String s : getNamesFromJarFile(locationDir.getPath())) {
                    if (!s.contains(".json")) {
                        continue;
                    } //The method grabbed a directory here.
                    if (!namesToBeOmitted.contains(s)) {
                        File destDir = new File(guideDirectory.getAbsoluteFile() + File.separator + "locations");
                        destDir.mkdirs(); //Will fail silently if it already exists
                        String newName = s.substring(0,s.length()-5);
                        File destFile = new File(destDir + File.separator + newName);
                        if (!destFile.exists()) {
                            File guideFile = new File("/guide/locations/" + s);
                            copyFileFromJar(guideFile, destFile);
                        } //else: it already exists, so dont touch it
                    }
                }
            }

            //items
            directory = getPackDirectory("items", packUsed);
            for (String type : getNamesFromJarFile(itemDir.getPath())) {
                if (type.contains(".txt") || type.contains(".json")) {
                    continue;
                } //Ignore it. This is not a directory.
                if (type.equalsIgnoreCase("/weapons/")) {
                    File weaponsDir = new File(directory + "/weapons");
                    File jarDir = new File(itemDir.getPath() + "/weapons");
                    namesToBeOmitted = new ArrayList<>();
                    if (directory != null) {
                        namesToBeOmitted = getOmittedAssets(new File(weaponsDir + File.separator + "omit.txt"));
                    }
                    if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                        for (String s : getNamesFromJarFile(jarDir.getPath())) {
                            if (s.endsWith("/")) {
                                continue;
                            } //It is a directory so ignore it
                            if (!namesToBeOmitted.contains(s)) {
                                File destDir = new File(guideDirectory.getAbsoluteFile() + File.separator + "items" + File.separator + "weapons");
                                destDir.mkdirs(); //Will fail silently if it already exists so we vibin
                                String newName = s.substring(0,s.length()-5);
                                File destFile = new File(destDir + File.separator + newName);
                                if (!destFile.exists()) {
                                    copyFileFromJar(new File("/guide/items/weapons/" + s), destFile);
                                } //else: it already exists, so dont touch it
                            }
                        }
                    }
                } else if (type.equalsIgnoreCase("/tools/")) {
                    File toolsDir = new File(directory + "/tools");
                    File jarDir = new File(itemDir.getPath() + "/tools");
                    namesToBeOmitted = new ArrayList<>();
                    if (directory != null) {
                        namesToBeOmitted = getOmittedAssets(new File(toolsDir + File.separator + "omit.txt"));
                    }
                    if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                        for (String s : getNamesFromJarFile(jarDir.getPath())) {
                            if (s.endsWith("/")) {
                                continue;
                            } //It is a directory so ignore it
                            if (!namesToBeOmitted.contains(s)) {
                                File destDir = new File(guideDirectory.getAbsoluteFile() + File.separator + "items" + File.separator + "tools");
                                destDir.mkdirs(); //Will fail silently if it already exists so we vibin
                                String newName = s.substring(0,s.length()-5);
                                File destFile = new File(destDir + File.separator + newName);
                                if (!destFile.exists()) {
                                    copyFileFromJar(new File("/guide/items/tools/" + s), destFile);
                                } //else: it already exists, so dont touch it
                            }
                        }
                    }
                } else if (type.equalsIgnoreCase("/armor/")) {
                    File armorDir = new File(directory + "/armor");
                    File jarDir = new File(itemDir.getPath() + "/armor/");
                    namesToBeOmitted = new ArrayList<>();
                    if (directory != null) {
                        namesToBeOmitted = getOmittedAssets(new File(armorDir + File.separator + "omit.txt"));
                    }
                    if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                        for (String s : getNamesFromJarFile(jarDir.getPath())) {
                            if (s.endsWith("/")) {
                                continue;
                            } //It is a directory so ignore it
                            if (!namesToBeOmitted.contains(s)) {
                                File destDir = new File(guideDirectory.getAbsoluteFile() + File.separator + "items" + File.separator + "armor");
                                destDir.mkdirs(); //Will fail silently if it already exists so we vibin
                                String newName = s.substring(0,s.length()-5);
                                File destFile = new File(destDir + File.separator + newName);
                                if (!destFile.exists()) {
                                    copyFileFromJar(new File("/guide/items/armor/" + s), destFile);
                                } //else: it already exists, so dont touch it
                            }
                        }
                    }
                } else if (type.equalsIgnoreCase("/specialitems/")) {
                    File specialitemsDir = new File(directory + "/specialitems");
                    File jarDir = new File(itemDir.getPath() + "/specialitems");
                    namesToBeOmitted = new ArrayList<>();
                    if (directory != null) {
                        namesToBeOmitted = getOmittedAssets(new File(specialitemsDir + File.separator + "omit.txt"));
                    }
                    if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                        for (String s : getNamesFromJarFile(jarDir.getPath())) {
                            if (s.endsWith("/")) {
                                continue;
                            } //It is a directory so ignore it
                            if (!namesToBeOmitted.contains(s)) {
                                File destDir = new File(guideDirectory.getAbsolutePath() + File.separator + "items" + File.separator + "specialitems");
                                destDir.mkdirs(); //Will fail silently if it already exists so we vibin
                                String newName = s.substring(0,s.length()-5);
                                File destFile = new File(destDir + File.separator + newName);
                                if (!destFile.exists()) {
                                    copyFileFromJar(new File("/guide/items/specialitems/" + s), destFile);
                                } //else: it already exists, so dont touch it
                            }
                        }
                    }
                }
            }

            //Enemies
            directory = getPackDirectory("enemies", packUsed);
            namesToBeOmitted = new ArrayList<>();
            if (directory != null) {
                namesToBeOmitted = getOmittedAssets(new File(directory + File.separator + "omit.txt"));
            }
            if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                for (String s : getNamesFromJarFile(enemyDir.getPath())) {
                    if (!s.contains(".json")) {
                        continue;
                    } //The method grabbed a directory here.
                    if (!namesToBeOmitted.contains(s)) {
                        File destDir = new File(guideDirectory.getAbsolutePath() + File.separator + "enemies");
                        destDir.mkdirs(); //Will fail silently if it already exists
                        String newName = s.substring(0,s.length()-5);
                        File destFile = new File(destDir + File.separator + newName);
                        if (!destFile.exists()) {
                            copyFileFromJar(new File("/guide/enemies/" + s), destFile);
                        } //else: it already exists, so dont touch it
                    }
                }
            }

            //Achievements
            directory = getPackDirectory("achievements", packUsed);
            namesToBeOmitted = new ArrayList<>();
            if (directory != null) {
                namesToBeOmitted = getOmittedAssets(new File(directory + File.separator + "omit.txt"));
            }
            if (!namesToBeOmitted.contains("%all%")) { //If all were omitted then dont worry about this.
                for (String s : getNamesFromJarFile(achievementDir.getPath())) {
                    if (!s.contains(".json")) {
                        continue;
                    } //The method grabbed a directory here.
                    if (!namesToBeOmitted.contains(s)) {
                        File destDir = new File(guideDirectory.getAbsolutePath() + File.separator + "achievements");
                        destDir.mkdirs(); //Will fail silently if it already exists
                        String newName = s.substring(0,s.length()-5);
                        File destFile = new File(destDir + File.separator + newName);
                        if (!destFile.exists()) {
                            copyFileFromJar(new File("/guide/achievements/" + s), destFile);
                        } //else: it already exists, so dont touch it
                    }
                }
            }

            /*For the following, we must figure out if they exist first. If not, then copy them.*/
            //death

            File file = getPackFile("death", guideDirectory);
            if (file == null) {
                copyFileFromJar(new File("/guide/death.txt"), new File(guideDirectory + "/death"));
            }

            //player

            file = getPackFile("player", guideDirectory);
            if (file == null) {
                copyFileFromJar(new File("/guide/player.txt"), new File(guideDirectory + "/player"));
            }

            //levelup

            file = getPackFile("levelup", guideDirectory);
            if (file == null) {
                copyFileFromJar(new File("/guide/levelup.txt"), new File(guideDirectory + "/levelup"));
            }
        }
    }

    /**
     * Copies the vanilla textfighter guide to the installation directory.
     * @return      Success or failure
     */
    public static boolean copyVanillaGuide() {
        Display.displayProgressMessage("Copying the vanilla textfighter guide to the installation location.");
        String jarGuideDir = "/guide";
        try {
            final File jarFile = new File(TextFighter.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            if(jarFile.isFile()) {  // Run with JAR file
                final JarFile jar = new JarFile(jarFile);
                final Enumeration<JarEntry> entries = jar.entries();
                while(entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.startsWith(jarGuideDir.substring(1))) { //filter according to the path and json file
                        if(name.endsWith(".txt")) { //It is a file, so we should copy it
                            Display.displayProgressMessage("Copying file: " + name);
                            InputStream stream = TextFighter.class.getResourceAsStream("/" + name); //Note the forward-slash
                            Files.copy(stream, Paths.get(installationRoot.getPath() + File.separator + name.substring(0, name.length()-4)), StandardCopyOption.REPLACE_EXISTING); //Note this removes the .txt file extension
                            stream.close();
                        } else { //It is a directory, so we should create an identical one
                            File directory = new File(installationRoot.getPath() + File.separator + name);
                            Display.displayProgressMessage("Copying Directory: " + name);
                            if(!directory.exists() && !directory.mkdirs()) {
                                Display.displayError("Unable to create a directory.");
                                return false;
                            }
                        }

                    }
                }
                jar.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    /**
     * Completely clears an entire directory
     * @param directory      The directory to clear
     */
    public static void clearDirectory(File directory) {
        if(directory == null || !directory.exists()) { return; } //I gave a completely invalid directory
        for (String f : Objects.requireNonNull(directory.list())) {
            File file = new File(directory.getAbsolutePath() + File.separatorChar + f);
            if (file.exists() && file.isDirectory()) {
                clearDirectory(file);
                directory.delete();
            } else if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * Copies the file given from the jar to an external directory.
     * @param fileInJar         The file to copy out of the jar.
     * @param dest              The destination for the copied file.
     */
    public static void copyFileFromJar(File fileInJar, File dest) {

        fileInJar = new File(fileInJar.getPath().replace(".json", ".txt"));
        dest = new File(dest.getPath().replace(".json",""));
        Display.displayProgressMessage("Copying File: " + fileInJar + " to " + dest.getAbsolutePath());
        try {
            new File(dest.getParent()).mkdirs();
            InputStream stream = TextFighter.class.getResourceAsStream(fileInJar.getPath()); //Note the forward-slash
            Files.copy(stream, Paths.get(dest.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
            stream.close();
        } catch(IOException | NullPointerException e) {
            Display.displayError("Unable to copy file " + fileInJar + " to " + dest);
        }
    }

    // guide/locations/inventory.txt
    // /guide/locations/inventory.txt

    /**
     * Gets a location from the {@link #locations} arraylist
     * @param name  the name of the location that is to be found
     * @return      If a location is found, then return it. Else, return null.
     */
    public static Location getLocationByName(String name) { for(Location l : locations) { if(l.getName().equals(name)) { return l; } } addToOutput("No such location '" + name + "'"); return null; }

    /**
     * Gets a weapon from the {@link #weapons} arraylist.
     * @param name  the name of the weapon that is to be found.
     * @return      If a weapon is found, then return it. Else, return null.
     */
    public static Weapon getWeaponByName(String name) { for(Weapon w : weapons) { if(w.getName().equals(name)) { return w; } } addToOutput("No such weapon '" + name + "'"); return null; }
    /**
     * Gets an armor from the {@link #armors} arraylist.
     * @param name  the name of the armor that is to be found.
     * @return      If a armor is found, then return it. Else, return null.
     */
    public static Armor getArmorByName(String name) { for(Armor a : armors) { if(a.getName().equals(name)) { return a; } } addToOutput("No such armor '" + name + "'"); return null; }
    /**
     * Gets a tool from the {@link #tools} arraylist.
     * @param name  the name of the tool that is to be found.
     * @return      If a tool is found, then return it. Else, return null.
     */
    public static Tool getToolByName(String name) { for(Tool t : tools) { if(t.getName().equals(name)) { return t; } } addToOutput("No such tool '" + name + "'"); return null; }
    /**
     * Gets a special item from the {@link #specialItems} arraylist.
     * @param name  the name of the special item that is to be found.
     * @return      If a special item is found, then return it. Else, return null.
     */
    public static SpecialItem getSpecialItemByName(String name) { for(SpecialItem sp : specialItems) { if(sp.getName().equals(name)) { return sp; } } addToOutput("No such specialitem '" + name + "'"); return null; }
    /**
     * Gets an enemy from the {@link #enemies} arraylist
     * @param name  the name of the enemy that is to be found
     * @return      If a enemy is found, then return it. Else, return null.
     */
    public static Enemy getEnemyByName(String name) { for(Enemy e : enemies) { if(e.getName().equals(name)) { return e; } } addToOutput("No such enemy '" + name + "'"); return null; }

    /**
     * Add a string to the {@link #output}.
     * <p>If the string given is null, then do not do anything.</p>
     * @param msg   The string that is to be added to the {@link #output}.
     */
    public static void addToOutput(String msg) { if(msg != null) { output+=msg + "\n";}  }

    /**
     * Return {@link #modName} if one is used. Else return "-".
     * @return      {@link #modName} if one is used. Else return "-".
     */
    public static String getModName() {
        if(modName != null){
            return modName;
        } else {
            return "-";
        }
    }

    /**
     * Get the version from the installation location
     * @return      The version read from the installation version file.
     */
    public static String readVersionFromInstallationLocation() {
        if(!installationVersionFile.exists()) { return "Not installed"; }
        try (BufferedReader br = new BufferedReader(new FileReader(installationVersionFile))) {
            return br.readLine();
        } catch (IOException | NullPointerException e) { e.printStackTrace(); return "Unknown"; }
    }

    /**
     * Get the version from the {@link #versionFile}.
     * @return      returns the version that is read. If no version was read, then returns "unknown".
     */
    public static String readVersionFromFile() {
        InputStream stream = TextFighter.class.getResourceAsStream("/VERSION.txt");
        if(stream == null) { Display.displayError("Difficulty reading the version file."); return "Unknown"; }
        Scanner scan = new Scanner(stream);
        String line = scan.next();
        if(line != null) { return line; } else { return "Unknown"; }
    }

    /**
     * Get the version of the game from {@link #version}.
     * @return      The {@link #version}.
     */
    public static String getVersion() { return version; }

    /**
     * Returns a group of json files as strings for parsing from a jar.
     * @return      the json files represented as strings.
     * @param path  The path within the jar file that all files within should be collected from.
     */
    public static ArrayList<String> getJsonFilesAsString(String path) {
        ArrayList<String> jsonStrings = new ArrayList<>();
        if(runFromJar) {
            try {
                final File jarFile = new File(TextFighter.class.getProtectionDomain().getCodeSource().getLocation().getPath());
                if (jarFile.isFile()) {  // Run with JAR file
                    final JarFile jar = new JarFile(jarFile);
                    final Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(path.substring(1) + "/") && name.endsWith(".json")) { //filter according to the path and json file
                            String json;
                            InputStream stream = TextFighter.class.getResourceAsStream("/" + name); //Note the forward-slash
                            if (stream == null) {
                                continue;
                            }
                            Scanner scan = new Scanner(stream).useDelimiter("\\Z");
                            try {
                                json = scan.next();
                            } catch (NoSuchElementException e) {
                                json = "{}";
                            } //This means the file is empty and we should pretty much ignore it
                            scan.close();
                            stream.close();
                            jsonStrings.add(json);
                        }
                    }
                    jar.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            File dir = new File("target" + File.separator  + "classes" + File.separator + path);
            if(!dir.exists() && !dir.isDirectory()) {
                Display.displayError("Directory given does not exist (not loading from jar)");
                return new ArrayList<>(Collections.singletonList("{}"));
            }
            try {
                for(String name : Objects.requireNonNull(dir.list())) {
                    String filename = dir.getAbsolutePath() + File.separator + name;
                    String json;
                    File file = new File(filename);
                    Scanner scan = new Scanner(file).useDelimiter("\\Z");
                    try {
                        json = scan.next();
                    } catch (NoSuchElementException e) {
                        json = "{}";
                    } //This means the file is empty and we should pretty much ignore it
                    scan.close();
                    jsonStrings.add(json);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jsonStrings;
    }

    /**
     * Returns a group of json files as strings for parsing from a jar.
     * @return      the json files represented as strings.
     * @param path  The path to the file within the jar.
     */
    public static String getSingleJsonFileAsString(String path) {
        String jsonString;
        if(runFromJar) {
            InputStream stream = TextFighter.class.getResourceAsStream(path);
            if (stream == null) {
                return "{}";
            }
            try (Scanner scan = new Scanner(stream).useDelimiter("\\Z")){
                jsonString = scan.next();
            } catch (NoSuchElementException e) {jsonString = " { }"; } //I know this is bad

            if (jsonString.isEmpty()) {
                return "{}";
            }
        } else {
            String filename = "target" + File.separator + "classes" + File.separator + path;
            File file = new File(filename);
            try {

                Scanner scan = new Scanner(file).useDelimiter("\\Z");
                try {
                    jsonString = scan.next();
                } catch (NoSuchElementException e) {
                    jsonString = "{}";
                } //This means the file is empty and we should pretty much ignore it
                scan.close();
            } catch (FileNotFoundException e) {
                Display.displayError("file '" + file.getAbsolutePath() + "' does not exist. Unable to get json from it.");
                return "{}";
            }
        }
        return jsonString;
    }

    /**Defines all resource directories*/
    public static void loadDirectories() {
        //Loads all the directories
        if(runFromJar) { //then the game is run from a jar
            assetsDir = new File("/assets");
            tagFile = new File(assetsDir.getPath() + "/" + "tags/tags.json");
            defaultValuesDirectory = new File(assetsDir.getPath() + "/" + "defaultvalues");
            interfaceDir = new File(assetsDir.getPath() + "/" + "userInterfaces/");
            locationDir = new File(assetsDir.getPath() + "/" + "locations");
            enemyDir = new File(assetsDir.getPath() + "/" + "enemies");
            achievementDir = new File(assetsDir.getPath() + "/" + "achievements");
            itemDir = new File(assetsDir.getPath() + "/" + "items");
            //configDir = new File("config");
            //packFile = new File(configDir.getPath() + File.separator + "pack");
            //packDir = new File("packs");
            customVariablesDir = new File(assetsDir.getPath() + "/" + "customvariables");
            deathmethodsFile = new File(assetsDir.getPath() + "/" + "deathmethods/deathmethods.json");
            levelupmethodsFile = new File(assetsDir.getPath() + "/" + "levelupmethods/levelupmethods.json");
            choicesOfAllLocationsFile = new File(assetsDir.getPath() + "/" + "choicesOfAllLocations/choicesOfAllLocations.json");
        } else {
            assetsDir = new File("assets");
            tagFile = new File(assetsDir.getPath() + File.separator + "tags/tags.json");
            defaultValuesDirectory = new File(assetsDir.getPath() + File.separator + "defaultvalues");
            interfaceDir = new File(assetsDir.getPath() + File.separator + "userInterfaces/");
            locationDir = new File(assetsDir.getPath() + File.separator + "locations");
            enemyDir = new File(assetsDir.getPath() + File.separator + "enemies");
            achievementDir = new File(assetsDir.getPath() + "/" + "achievements");
            itemDir = new File(assetsDir.getPath() + File.separator + "items");
            //configDir = new File("config");
            //packFile = new File(configDir.getPath() + File.separator + "pack");
            //packDir = new File("packs");
            customVariablesDir = new File(assetsDir.getPath() + File.separator + "customvariables");
            deathmethodsFile = new File(assetsDir.getPath() + File.separator + "deathmethods/deathmethods.json");
            levelupmethodsFile = new File(assetsDir.getPath() + File.separator + "levelupmethods/levelupmethods.json");
            choicesOfAllLocationsFile = new File(assetsDir.getPath() + File.separator + "choicesOfAllLocations/choicesOfAllLocations.json");
        }
    }

    /**
     * Defines all resource directories then loads all the assets needed for the game.
     * @return      True if successful. False is unsuccessful.
     */
    public static boolean loadassets() {
        //Load some things
        loadConfig();
        //loads the content
        //If any of the necessary content fails to load, send an error and exit the game
        Display.displayProgressMessage("Loading the assets...");
        return loadDefaultValues() && loadCustomVariables() && loadDeathMethods() && loadLevelUpMethods() && loadItems() && loadParsingTags() && loadInterfaces() && loadChoicesOfAllLocations() && loadLocations() && loadEnemies() && loadAchievements();
    }

    /**
     * Loads a singular method.
     * <p> Useful for loading a Method that is an argument that you wouldnt/cant pass a JSONArray to </p>
     * @param type          The type of the method.
     * @param obj           The JSONObject that the method is located in.
     * @param parentType    The class of the parent of the method.
     * @return              The new method that was loaded.
    */
    public static Object loadMethod(Class type, JSONObject obj, Class parentType) {
        ArrayList<Object> argumentsRaw = (JSONArray)obj.get("arguments");
        ArrayList<String> argumentTypesString = (JSONArray)obj.get("argumentTypes");
        ArrayList<Class> argumentTypes = new ArrayList<>();
        ArrayList<Class> parameterArgumentTypes = new ArrayList<>(); //Used for getting methods that need Object as a parameter type
        if(type.equals(UiTag.class) && obj.get("tag") != null) { Display.displayPackMessage("Loading tag '" + obj.get("tag") + "'"); }
        String methodString = (String)obj.get("method");
        Display.displayPackMessage("Loading method '" + methodString + "' of type '" + type.getSimpleName() + "'");
        Display.changePackTabbing(true);
        String rawClass = (String)obj.get("class");
        Object rawField = obj.get("field");
        String rawFieldClass = (String)obj.get("fieldclass");
        if(rawField != null) {
            if(rawField.getClass().equals(JSONObject.class)) {
                rawField = loadMethod(FieldMethod.class, (JSONObject)rawField, type);
            }
        }
        if(methodString == null || rawClass == null) { Display.displayPackError("This method has no class or method. Omitting..."); Display.changePackTabbing(false); return null; }
        if(!((argumentsRaw != null && argumentTypesString != null) && (argumentsRaw.size() == argumentTypesString.size()) || (argumentTypesString == null && argumentsRaw == null))) { Display.displayPackError("This method does not have the same amount of arguments as argumentTypes. Omitting..."); Display.changePackTabbing(false); return null; }
        //Fields and fieldclasses can be null (Which just means the method does not act upon a field
        if(argumentTypesString != null && argumentTypesString.size() > 0) {
            //Translates the raw argument types to classes
            //0 is String, 1 is int, 2 is double, 3 is boolean, 4 is class
            for (int g=0; g<argumentTypesString.size(); g++) {
                String argType = argumentTypesString.get(g);
                if(argType == null) { Display.displayPackError("This methods has an empty value for an argument. Omitting..."); Display.changePackTabbing(false); return null; }
                // Check to see if the method recieving these arguments wants an Object
                boolean isObject = false;
                if(argType.length() > 1 && argType.startsWith("*")) {
                    parameterArgumentTypes.add(Object.class);
                    isObject = true;
                    argumentTypesString.set(g, argType.substring(1));
                }
                int num = Integer.parseInt(argumentTypesString.get(g));
                if(num == 0) {
                    argumentTypes.add(String.class);
                    if(!isObject) { parameterArgumentTypes.add(String.class); }
                } else if(num == 1) {
                    argumentTypes.add(int.class);
                    if(!isObject) { parameterArgumentTypes.add(int.class); }
                } else if(num == 2) {
                    argumentTypes.add(double.class);
                    if(!isObject) { parameterArgumentTypes.add(double.class); }
                } else if(num == 3) {
                    argumentTypes.add(boolean.class);
                    if(!isObject) { parameterArgumentTypes.add(boolean.class); }
                } else if(num == 4) {
                    argumentTypes.add(Class.class);
                    if(!isObject) { parameterArgumentTypes.add(Class.class); }
                } else {
                    Display.displayPackError("This method has arguments that are not String, int, double, boolean, or class. Omitting...");
                    Display.changePackTabbing(false);
                }
            }
        }

        //Gets the class
        Class clazz;
        try {
            clazz = Class.forName(rawClass);
            if(rawField instanceof FieldMethod) {
                if(!((FieldMethod)rawField).getMethod().getReturnType().equals(clazz) && !((FieldMethod)rawField).getMethod().getReturnType().isAssignableFrom(clazz) && clazz.isAssignableFrom(((FieldMethod)rawField).getMethod().getReturnType())) {
                    Display.displayPackError("This field method given does not have a return type of the class in the 'class' key. Omitting..."); Display.changePackTabbing(false); return null;
                }
            }
        } catch (NoClassDefFoundError | ClassNotFoundException e){ Display.displayPackError("This method has an invalid class. Make sure the class is not private. Omitting..."); Display.changePackTabbing(false); return null; }

        Class fieldclass;
        Object field = null;

        //If the field is not a method, and no fieldclass is given, then omit the method
        if(rawField instanceof String && rawFieldClass == null) {
            Display.displayPackError("This method has a non-method field, but no fieldclass. Omitting..."); Display.changePackTabbing(false); return null;
        }

        if(rawField instanceof String) {
            //Gets the fieldclass
            try { fieldclass = Class.forName(rawFieldClass); } catch (ClassNotFoundException e){ Display.displayPackError("This method has an invalid fieldclass. Omitting..."); Display.changePackTabbing(false); return null; }
            //Gets the field from the class
            try {
                field = fieldclass.getField((String)rawField);
                if(!((Field)field).getType().equals(clazz)) { Display.displayPackError("This method's field is not an instance of the class given in the 'class' key. Omitting..."); Display.changePackTabbing(false); return null; }
            } catch (NoSuchFieldException | SecurityException e) { Display.displayPackError("This method has an invalid field. Make sure the field is not private. Omitting..."); Display.changePackTabbing(false); return null; }
        } else if (rawField != null){
            field = rawField;
        }

        //Gets the method from the class
        Method method;
        try {
            method = clazz.getMethod(methodString, parameterArgumentTypes.toArray(new Class[0])); //parameterArgumentTypes.size()
        } catch (NoSuchMethodException e){ Display.displayPackError("Method '" + methodString + "' of class '" + rawClass + "' with given arguments could not be found. Make sure the method is not private. Omitting..."); Display.changePackTabbing(false); return null; }

        //Makes the arguments the correct type (String, int, or boolean)
        ArrayList<Object> arguments = new ArrayList<>();
        if(argumentsRaw != null) {
            //Cast all of the arguments to the correct type
            for (int p=0; p<argumentTypes.size(); p++) {
                if(argumentsRaw.get(p).getClass().equals(JSONObject.class)) {
					arguments.add(loadMethod(TFMethod.class, (JSONObject)argumentsRaw.get(p), type));
                } else {
                    //The syntax for using a null value is "%null%"
                    if((argumentsRaw.get(p)).equals("%null%")) { arguments.add(null); }
                    if(((String)argumentsRaw.get(p)).contains("%ph%")) { arguments.add(argumentsRaw.get(p)); }
                    else if(argumentTypes.get(p).equals(int.class)) {
                        arguments.add(Integer.parseInt((String)argumentsRaw.get(p)));
                    } else if (argumentTypes.get(p).equals(String.class)) {
                        arguments.add(argumentsRaw.get(p));
                    } else if(argumentTypes.get(p).equals(double.class)) {
                        arguments.add(Double.parseDouble((String)argumentsRaw.get(p)));
                    } else if (argumentTypes.get(p).equals(boolean.class)) {
                        arguments.add(Boolean.parseBoolean((String)argumentsRaw.get(p)));
                    } else if (argumentTypes.get(p).equals(Class.class)) {
                        try { arguments.add(Class.forName((String)argumentsRaw.get(p))); } catch(ClassNotFoundException e) { Display.displayPackError("An argument tried to get a class that does not exist. Omitting... "); Display.changePackTabbing(false); return null; }
                    } else {
                        Display.displayPackError("This method has arguments that are not String, int, double, boolean, or class. Omitting...");
                        Display.changePackTabbing(false);
                    }
                }
            }
        }

        Object o = null;
        //Uses null here because if the type is not any of the following, then return null.

        //Creates the correct Method type
        if(type.equals(ChoiceMethod.class)) {
            o=new ChoiceMethod(method, arguments, argumentTypes, field, loadMethods(Requirement.class, (JSONArray)obj.get("requirements"), ChoiceMethod.class));
        } else if(type.equals(Requirement.class)) {
            boolean neededBoolean = true; if(obj.get("neededBoolean") != null) {neededBoolean=Boolean.parseBoolean((String)obj.get("neededBoolean"));}
            o=new Requirement(method, arguments, argumentTypes, field, neededBoolean);
        } else if(type.equals(EnemyActionMethod.class)) {
            o=new EnemyActionMethod(method, arguments, argumentTypes, field);
        } else if(type.equals(TFMethod.class)) {
            o=new TFMethod(method, arguments, argumentTypes, field, loadMethods(Requirement.class, (JSONArray)obj.get("requirements"), TFMethod.class));
        } else if(type.equals(Reward.class)) {
            if(parentType.equals(Enemy.class)) {
                int chance = Reward.defaultChance; if(obj.get("chance") != null){chance=Integer.parseInt((String)obj.get("chance"));}
                if(chance == 0) { Display.displayPackError("This reward has no chance. Omitting..."); return null; }
                o=new Reward(method, arguments, argumentTypes, field, loadMethods(Requirement.class, (JSONArray)obj.get("requirements"), Enemy.class), chance, (String)obj.get("rewarditem"));
            } else {
                //Achievement rewards always run (So no chance needed)
                o=new Reward(method, arguments, argumentTypes, field, loadMethods(Requirement.class, (JSONArray)obj.get("requirements"), Achievement.class), 100, (String)obj.get("rewarditem"));
            }
        } else if(type.equals(UiTag.class)) {
            String tag = (String)obj.get("tag");
            if(tag == null) { Display.displayPackError("This tag has no tagname. Omitting..."); Display.changePackTabbing(false); return null; }
            o=new UiTag(tag, method, arguments, argumentTypes, field, loadMethods(Requirement.class, (JSONArray)obj.get("requirements"), UiTag.class));
        } else if(type.equals(FieldMethod.class)) {
            o=new FieldMethod(method, arguments, argumentTypes, field);
        }
        Display.changePackTabbing(false);
        return o;
    }

    /**
     * Loads a JSONArray of methods
     * <p>Loads the methods in things like requirements, choicemethods, and others
     * Used if you have a JSONArray full of methods (And only methods) to parse</p>
     * @param type          The type of methods that are being loaded (ChoiceMethod, Requirement, etc.).
     * @param methodArray   The JSONArray that the methods are located in.
     * @param parentType    The class of the parent of the methods.
     * @return              An ArrayList of the type of methods loaded.
    */
    public static ArrayList loadMethods(Class type, JSONArray methodArray, Class parentType) {
        ArrayList<Object> methods = new ArrayList<>();
        if((methodArray != null && methodArray.size() > 0)) {
            for (Object value : methodArray) {
                JSONObject o = (JSONObject) value;
                Object method = loadMethod(type, o, parentType);
                if (method != null) {
                    methods.add(method);
                }
            }
            return methods;
        } else { return null; }
    }

    /**
     * Returns a directory with the given name in the directory (parent) given.
     * @param directoryName     The name of the directory to find
     * @param parent            The parent directory
     * @return                  A directory with the given name in the directory (parent) given. If none found, return null.
     */
    public static File getPackDirectory(String directoryName, File parent) {
        if(parent != null && parent.exists() && parent.isDirectory()) {
            for(String s : Objects.requireNonNull(parent.list())) {
                if(s.equals(directoryName)) {
                    File pack = new File(parent.getPath() + File.separator + directoryName);
                    if(pack.isDirectory()) {
                        if(parsingPack && packUsed != null) {  Display.displayPackMessage("loading " + directoryName + " from pack '" + packUsed.getName() + "'"); }
                        return pack;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a file with the given name in the directory (parent) given.
     * @param fileName      The name of the file to find
     * @param parent            The parent directory
     * @return      A file with the given name in the directory (parent) given. If none found, return null.
     */
    public static File getPackFile(String fileName, File parent) {
        if(parent != null && parent.exists() && parent.isDirectory()) {
            for(String s : Objects.requireNonNull(parent.list())) {
                if(s.equals(fileName)) {
                    File pack = new File(parent.getPath() + File.separator + fileName);
                    if(pack.isFile()) {
                        if(parsingPack && packUsed != null) { Display.displayPackMessage("loading " + fileName + " from pack '" + packUsed.getName() + "'"); }
                        return pack;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns all names listed in the omit.txt file of the pack directory given.
     * @param file          The file that contains the omitted asset names.
     * @return              all names listed in the omit.txt file of the pack directory given.
     */
    public static ArrayList<String> getOmittedAssets(File file) {
        ArrayList<String> omittedAssets = new ArrayList<>();
        if(!file.exists()) { return omittedAssets; }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                omittedAssets.add(line);
            }
        } catch (IOException e) { Display.displayWarning("IOException when attempting to read the omit file (The file does exist). Continuing normally..."); }
        return omittedAssets;
    }

    /**
     * Loads the interfaces.
     * <p>First loads from the pack (If one is specified in the config file), then loads from the default pack.
     * @return      True if successful, False if unsuccessful
     */
    public static boolean loadInterfaces() {
        parsingPack = false;
        Display.displayProgressMessage("Loading the interfaces...");
        Display.changePackTabbing(true);
        /*
        if (!interfaceDir.exists()) {
            Display.displayError("Could not find the default interfaces directory.");
            Display.changePackTabbing(false);
            return false;
        }*/
        ArrayList<String> usedNames = new ArrayList<>(); // Used to override default interfaces with ones in packs
        File directory = interfaceDir;

        //Determine if there is a pack to be loaded and start loading from it if there is
        File packDirectory = getPackDirectory("userInterfaces", packUsed);
        if (packDirectory != null) { directory = packDirectory; parsingPack = true; }

        //Place where all names that are located in the omit file are stored
        ArrayList<String> namesToBeOmitted = getOmittedAssets(new File(directory + File.separator + "omit.txt"));

        //Now parses the interfaces, gets from the modpack first then the default pack
        for (int num = 0; num < 2; num++) {

            ArrayList<String> jsonStrings;

            //Get all the assets from the mod and the default assets
            if (!parsingPack) {
                num++;
                Display.displayPackMessage("Loading userinterfaces from the default pack.");
                jsonStrings = getJsonFilesAsString(directory.getPath().replace("\\", "/"));
            } else {
                Display.displayPackMessage("Loading from modpack");
                jsonStrings = new ArrayList<>();
                for (String s : Objects.requireNonNull(directory.list())) {
                    try {

                        try (Scanner scan = new Scanner(new File(directory + File.separator + s)).useDelimiter("\\Z")) {
                            jsonStrings.add(scan.next());
                        } catch(NoSuchElementException ignored) { } //I know this is bad
                    } catch (IOException | NullPointerException e) {
                        e.printStackTrace();
                        Display.displayError("Error while reading file '" + s + "' from modpack.");
                    }
                }
            }

            for (String f : jsonStrings) {
                //Load the values from the file. If any value is null (That is required), then omit the interface
                JSONObject interfaceFile;
                try {
                    interfaceFile = (JSONObject) parser.parse(f);
                } catch (ParseException e) {
                    Display.displayError("Having trouble parsing interface file '" + f + "'");
                    continue;
                }
                if (interfaceFile == null) {
                    continue;
                }
                JSONArray interfaceArray = (JSONArray) interfaceFile.get("interface");
                String name = (String) interfaceFile.get("name");
                if ((usedNames.contains(name) || (!parsingPack && namesToBeOmitted.contains(name))) && !name.equals("choices")) {
                    continue;
                }
                Display.displayPackMessage("Loading interface '" + name + "'");
                Display.changePackTabbing(true);
                if (name == null) {
                    Display.displayPackError("This does not have a name. Omitting...");
                    Display.changePackTabbing(false);
                    continue;
                }
                if (interfaceArray == null) {
                    Display.displayPackError("This does not have an interface array. Omitting...");
                    Display.changePackTabbing(false);
                    continue;
                }
                StringBuilder uiString = new StringBuilder();
                for (int i = 0; i < interfaceArray.size(); i++) {
                    uiString.append(interfaceArray.get(i));
                    if(i != interfaceArray.size()-1) {
                        uiString.append("\n");
                    }
                }
                Display.interfaces.add(new UserInterface(name, uiString.toString()));
                usedNames.add(name);
                Display.changePackTabbing(false);
            }
            //Go parse the default pack (If it already was, then it wont matter if this runs or not)
            directory = interfaceDir;
            parsingPack = false;
            if (namesToBeOmitted.contains("%all%")) {
                break;
            }
        }
        boolean choicesInterfaceExists = false;
        for (UserInterface ui : Display.interfaces) {
            if (ui.getName().equals("choices")) {
                choicesInterfaceExists = true;
                break;
            }
        }
        if (!choicesInterfaceExists) {
            Display.displayError("There is no interface with the name 'choices'. The game cannot run without one, because the player needs to know what his/her choices are.");
            Display.changePackTabbing(false);
            return false;
        }
        Display.displayProgressMessage("Interfaces loaded.");
        Display.changePackTabbing(false);
        return true;
    }

    /**
     * Loads the locations.
     * <p>First loads from the pack (If one is specified in the config file), then loads from the default pack.
     * @return      True if successful, false if unsuccessful.
     */
    public static boolean loadLocations() {
        parsingPack = false;
        Display.displayProgressMessage("Loading the locations...");
        Display.changePackTabbing(true);
        //if(!locationDir.exists()) { Display.displayError("Could not find the default locations directory."); Display.changePackTabbing(false); return false;}
        ArrayList<String> usedNames = new ArrayList<>();
        File directory = locationDir;

        //Determine if there is a pack to be loaded and start loading from it if there is
        File packDirectory = getPackDirectory("locations", packUsed);
        if(packDirectory != null) { directory = packDirectory; parsingPack = true;}

        //Place where all names that are located in the omit file are stored
        ArrayList<String> namesToBeOmitted = getOmittedAssets(new File(directory + File.separator + "omit.txt"));

        //Load them
        for(int num=0; num<2; num++) {

            ArrayList<String> jsonStrings;

            //Get all the assets from the mod and the default assets
            if(!parsingPack) {
                num++; Display.displayPackMessage("Loading locations from the default pack.");
                jsonStrings = getJsonFilesAsString(directory.getPath().replace("\\", "/"));
            } else {
                Display.displayPackMessage("Loading from modpack");
                jsonStrings = new ArrayList<>();
                for (String s : Objects.requireNonNull(directory.list())) {
                    if(!s.endsWith(".json")) { continue; } //This is not a file we want to look at (an omit file or a directory)
                    try (Scanner scan = new Scanner(new File(directory + File.separator + s)).useDelimiter("\\Z")) {
                        jsonStrings.add(scan.next());
                    } catch(NoSuchElementException e) {
                    } catch (IOException | NullPointerException e) {
                        e.printStackTrace();
                        Display.displayError("Error while reading file '" + s + "' from modpack.");
                    }
                }

            }

            for(String f : jsonStrings) {
                //Load the values, if any are null (other than values that are not required), then omit the location
                JSONObject locationFile = null;
                try { locationFile = (JSONObject)parser.parse(f); } catch (ParseException e) { Display.displayPackError("Having trouble parsing from file"); e.printStackTrace(); Display.changePackTabbing(false);}
                if(locationFile == null) { continue; }
                JSONArray interfaceJArray = (JSONArray)locationFile.get("interfaces");
                String name = (String)locationFile.get("name");
                if(usedNames.contains(name) || (!parsingPack && namesToBeOmitted.contains(name))) { continue; }
                Display.displayPackMessage("Loading Location '" + name + "'");
                Display.changePackTabbing(true);
                if(name == null) { Display.displayPackError("This location does not have a name. Omitting..."); Display.changePackTabbing(false); continue; }
                if(interfaceJArray == null) { Display.displayPackError("Location '" + name + "' does not have any interfaces. Omitting..."); Display.changePackTabbing(false); continue; }
                ArrayList<UserInterface> interfaces = new ArrayList<>();
                boolean hasChoiceInterface = false;
                for (Object o : interfaceJArray) {
                    String interfaceName = (String) o;
                    boolean validInterface = false;
                    for (UserInterface ui : Display.interfaces) {
                        if (ui.getName().equals(interfaceName)) {
                            validInterface = true;
                            break;
                        }
                    }
                    if (!validInterface) {
                        Display.displayPackError("Unknown interface '" + interfaceName + "'");
                    }
                }
                //Determines if the location has a choices interface
                for (Object o : interfaceJArray) {
                    for (UserInterface ui : Display.interfaces) {
                        if (ui.getName().equals(o)) {
                            if (ui.getName().equals("choices")) {
                                hasChoiceInterface = true;
                            }
                            interfaces.add(ui);
                        }
                    }
                }
                //Automatically add the choices interface if it is not there
                if(!hasChoiceInterface) {
                    for(UserInterface ui : Display.interfaces) {
                        if(ui.getName().equals("choices")) {
                            interfaces.add(ui);
                            break;
                        }
                    }
                }
                JSONArray choiceArray = (JSONArray)locationFile.get("choices");
                ArrayList<Choice> choices = new ArrayList<>();
                ArrayList<String> usedChoiceNames = new ArrayList<>();
                //Loads the choices from the file
                if(choiceArray != null && choiceArray.size() > 0) {
                    for (Object o : choiceArray) {
                        JSONObject obj = (JSONObject) o;
                        String choicename = (String) obj.get("name");
                        Display.displayPackMessage("Loading choice '" + choicename + "'");
                        Display.changePackTabbing(true);
                        String desc = (String) obj.get("description");
                        String usage = (String) obj.get("usage");
                        if (obj.get("methods") == null) {
                            Display.displayPackError("This choice has no methods. Omitting...");
                            Display.changePackTabbing(false);
                            continue;
                        }
                        if (choicename == null) {
                            Display.displayPackError("Ths choice has no name. Omitting...");
                            Display.changePackTabbing(false);
                            continue;
                        }
                        Choice c = new Choice(choicename, desc, usage, loadMethods(ChoiceMethod.class, (JSONArray) obj.get("methods"), Choice.class), loadMethods(Requirement.class, (JSONArray) obj.get("requirements"), Choice.class), (String) obj.get("failMessage"));
                        if (c.getMethods() != null && c.getMethods().size() > 0) {
                            choices.add(c);
                        }
                        usedChoiceNames.add(choicename);
                        Display.changePackTabbing(false);
                    }
                }

                //Add any choices in the choicesOfAllLocations arraylist
                for(ChoiceOfAllLocations coal : choicesOfAllLocations) {
                    if(coal.getExcludedLocations().contains(name)) { continue; }
                    boolean choicesNameUsed = false;
                    for(Choice c : choices) {
                        if(c.getName().equals("choices")) {
                            choicesNameUsed = true;
                            break;
                        }
                    }
                    if(!choicesNameUsed) {
                        usedChoiceNames.add(coal.getChoice().getName());
                        choices.add(coal.getChoice());
                    }
                }

                //Adds the quit choice if it does not exist yet
                if(!usedChoiceNames.contains("quit")) {
                    Display.displayPackMessage("Adding the quit choice");
                    Display.changePackTabbing(true);

                    Method method;
                    try {
                        method = TextFighter.class.getMethod("exitGame", int.class);
                    } catch (NoSuchMethodException e){ Display.displayPackError("Cannot find method 'exitGame'. Omitting..."); Display.changePackTabbing(false); continue; }
                    ArrayList<Object> arguments = new ArrayList<>(); arguments.add(0);
                    ArrayList<Class> argumentTypes = new ArrayList<>(); argumentTypes.add(int.class);
                    ArrayList<ChoiceMethod> choiceMethods = new ArrayList<>(); choiceMethods.add(new ChoiceMethod(method, arguments, argumentTypes, null, null));
                    choices.add(new Choice("quit", "quits the game", "quit", choiceMethods, null, null));
                    Display.changePackTabbing(false);
                }
                //Adds the loaded location to the location ArrayList
                locations.add(new Location(name, interfaces, choices, loadMethods(TFMethod.class, (JSONArray)locationFile.get("premethods"), Location.class), loadMethods(TFMethod.class, (JSONArray)locationFile.get("postmethods"), Location.class)));
                usedNames.add(name);
                Display.changePackTabbing(false);
            }
            directory = locationDir;
            parsingPack = false;
            if(namesToBeOmitted.contains("%all%")) { break; }
        }
        boolean startGiven = false;
        boolean menuGiven = false;
        boolean fightGiven = false;
        for(Location l : locations) {
            if(l.getName().equals("start")) { startGiven = true; }
            if(l.getName().equals("menu")) { menuGiven = true; }
            if(l.getName().equals("fight")) { fightGiven = true; }
        }
        if(!startGiven) { Display.displayWarning("No start location was given."); }
        if(!menuGiven) { Display.displayWarning("No menu location was given."); }
        if(!fightGiven) { Display.displayWarning("No fight location was given."); }
        Display.displayProgressMessage("Locations loaded.");
        Display.changePackTabbing(false);
        return true;
    }

    /**
     * Loads the enemies.
     * <p>First loads from the pack (If one is specified in the config file), then loads from the default pack.
     * @return      True if successful, false if unsuccessful.
     */
    public static boolean loadEnemies() {
        parsingPack = false;
        Display.displayProgressMessage("Loading the enemies...");
        Display.changePackTabbing(true);
        //if(!enemyDir.exists()) { Display.displayError("Could not find the default enemies directory."); Display.changePackTabbing(false); return false;}
        ArrayList<String> usedNames = new ArrayList<>();
        File directory = enemyDir;

        //Determine if there is a pack to be loaded and start loading from it if there is
        File packDirectory = getPackDirectory("enemies", packUsed);
        if(packDirectory != null) { directory = packDirectory; parsingPack = true;}

        //Place where all names that are located in the omit file are stored
        ArrayList<String> namesToBeOmitted = getOmittedAssets(new File(directory + File.separator + "omit.txt"));

        for(int num=0; num<2; num++) {

            ArrayList<String> jsonStrings;

            //Get all the assets from the mod and the default assets
            if(!parsingPack) {
                num++; Display.displayPackMessage("Loading enemies from the default pack.");
                jsonStrings = getJsonFilesAsString(directory.getPath().replace("\\", "/"));
            } else {
                Display.displayPackMessage("Loading from modpack");
                jsonStrings = new ArrayList<>();
                for (String s : Objects.requireNonNull(directory.list())) {
                    if(!s.endsWith(".json")) { continue; } //This is not a file we want to look at (an omit file or a directory)
                    try (Scanner scan = new Scanner(new File(directory + File.separator + s)).useDelimiter("\\Z")) {
                            jsonStrings.add(scan.next());
                    } catch (NoSuchElementException ignore) {
                    } catch (IOException | NullPointerException e) {
                        e.printStackTrace();
                        Display.displayError("Error while reading file '" + s + "' from modpack.");
                    }
                }
            }
            for(String f : jsonStrings) {
                JSONObject enemyFile = null;
                try { enemyFile = (JSONObject) parser.parse(f); } catch (ParseException e) { Display.displayPackError("Having trouble parsing from file"); e.printStackTrace(); Display.changePackTabbing(false);}
                if(enemyFile == null) { continue; }
                //If a value is not specified in the file, it automatically goes to the default values
                String name = Enemy.defaultName;                        if(enemyFile.get("name") != null){              name=(String)enemyFile.get("name");}
                if(usedNames.contains(name) || (!parsingPack && namesToBeOmitted.contains(name))) { Display.changePackTabbing(false); continue; }
                Display.displayPackMessage("Loading enemy '" + name + "'");
                Display.changePackTabbing(true);
                String description = Enemy.defaultDescription;          if(enemyFile.get("description") != null){       description=(String)enemyFile.get("description");}
                int health = Enemy.defaultHp;                           if(enemyFile.get("health") != null){            health=Integer.parseInt((String)enemyFile.get("health"));}
                int maxhealth = Enemy.defaultMaxhp;                     if(enemyFile.get("maxhealth") != null){             maxhealth=Integer.parseInt((String)enemyFile.get("maxhealth"));}
                int strength = Enemy.defaultStrength;                   if(enemyFile.get("strength") != null){          strength=Integer.parseInt((String)enemyFile.get("strength"));}
                int levelRequirement =  Enemy.defaultLevelRequirement;  if(enemyFile.get("levelRequirement") != null){  levelRequirement=Integer.parseInt((String)enemyFile.get("levelRequirement"));}
                boolean finalBoss = false;                              if(enemyFile.get("finalBoss") != null){         finalBoss=Boolean.parseBoolean((String)enemyFile.get("finalBoss"));}
                if(levelRequirement < 1) { levelRequirement=1; }
                JSONArray enemyActionArray = (JSONArray)enemyFile.get("actions");
                ArrayList<EnemyAction> enemyActions = new ArrayList<>();
                if(enemyActionArray != null && enemyActionArray.size() > 0) {
                    for (Object o : enemyActionArray) {
                        JSONObject obj = (JSONObject) o;
                        EnemyAction a = new EnemyAction(loadMethods(EnemyActionMethod.class, (JSONArray) obj.get("methods"), EnemyAction.class), loadMethods(EnemyActionMethod.class, (JSONArray) obj.get("requirements"), Enemy.class));
                        if (a.getMethods() != null && a.getMethods().size() > 1) {
                            enemyActions.add(a);
                        }
                    }
                }
                //Add the enemy to the enemies arraylist
                enemies.add(new Enemy(name, description, health, maxhealth, strength, levelRequirement, finalBoss,
                            loadMethods(Requirement.class, (JSONArray)enemyFile.get("requirements"), Enemy.class),
                            loadMethods(TFMethod.class, (JSONArray)enemyFile.get("premethods"), Enemy.class),
                            loadMethods(TFMethod.class, (JSONArray)enemyFile.get("postmethods"), Enemy.class),
                            loadMethods(Reward.class, (JSONArray)enemyFile.get("rewards"), Enemy.class),
                            enemyActions, enemyCustomVariables));
                usedNames.add(name);
                Display.changePackTabbing(false);
            }
            directory=enemyDir;
            parsingPack=false;
            if(namesToBeOmitted.contains("%all%")) { break; }
        }
        sortEnemies();
        Display.displayProgressMessage("Enemies loaded.");
        Display.changePackTabbing(false);
        return true;
    }

    /**
     * Loads the parsing (interface) tags.
     * <p>First loads from the pack (If one is specified in the config file), then loads from the default pack.
     * @return      True if successful, false if unsuccessful.
     */
    public static boolean loadParsingTags() {
        parsingPack = false;
        //Custom parsing tags are located in interface packs
        Display.displayProgressMessage("Loading the parsing tags...");
        Display.changePackTabbing(true);
        //if(!tagFile.exists()) { Display.displayError("Could not find the default tags file."); Display.changePackTabbing(false); return false;}
        ArrayList<String> usedNames = new ArrayList<>();
        File file = tagFile;

        //Determine if there is a pack to be loaded from and start loading from it if there is
        File packDirectory = getPackDirectory("tags", packUsed);
        if(packDirectory != null && packDirectory.list() != null) {
            File newFile = new File(packDirectory + File.separator + "tags.json");
            if(newFile.exists()) { file = newFile; parsingPack = true; }
        }

        ArrayList<String> namesToBeOmitted = getOmittedAssets(new File(file.getParentFile().getAbsolutePath() + File.separator + "omit.txt"));

        //Load them
        for(int num=0; num<2; num++) {

            String jsonString = "{}"; //the tags

            //Get all the assets from the mod and the default assets
            if(!parsingPack) {
                num++; Display.displayPackMessage("Loading parsing tags from the default pack.");
                jsonString = getSingleJsonFileAsString(file.getPath().replace("\\", "/"));
            } else {
                Display.displayPackMessage("Loading from modpack");
                try {
                    Scanner scan = new Scanner(file.getAbsolutePath());
                    try { jsonString = scan.next(); } catch(NoSuchElementException e) { jsonString = "{}"; } //I know this is bad
                    scan.close();
                } catch (NullPointerException e) {
                    Display.displayError("Error while reading file '" + file.getAbsolutePath() + "' from modpack.");
                }
            }

            if(!parsingPack && namesToBeOmitted.contains("%all%")) { continue; }
            JSONObject tagsFile;
            try { tagsFile = (JSONObject)parser.parse(jsonString);  } catch (ParseException e) { Display.displayPackError("Having trouble parsing from tags file"); e.printStackTrace(); continue; }
            if(tagsFile == null) { continue; }
            JSONArray tagsArray = (JSONArray)tagsFile.get("tags");
            if(tagsArray == null) { continue; }
            //Load each of the tags
            for(Object obj1 : tagsArray) {
                JSONObject obj = (JSONObject)obj1;
                String tagName = (String)obj.get("tag");
                if(tagName != null) {
                    if (!(!parsingPack && namesToBeOmitted.contains(tagName) && !usedNames.contains(tagName))) {
                        UiTag tag = (UiTag)loadMethod(UiTag.class, obj, null);
                        if(tag != null) { Display.interfaceTags.add(tag); usedNames.add(tag.getTag()); }
                    }

                }
            }
            parsingPack = false;
            file = tagFile;
        }
        Display.displayProgressMessage("Parsing tags loaded.");
        Display.changePackTabbing(false);
        return true;
    }

    /**
     * Loads theachievements.
     * <p>First loads from the pack (If one is specified in the config file), then loads from the default pack.
     * @return      True if successful, false if unsuccessful.
     */
    public static boolean loadAchievements() {
        parsingPack = false;
        Display.displayProgressMessage("Loading the achievements...");
        Display.changePackTabbing(true);
        ArrayList<String> usedNames = new ArrayList<>();
        File directory = achievementDir;

        //Determine if there is a pack to be loaded and start loading from it if there is
        File packDirectory = getPackDirectory("achievements", packUsed);
        if(packDirectory != null) { directory = packDirectory; parsingPack = true;}

        ArrayList<String> namesToBeOmitted = getOmittedAssets(new File(directory + File.separator + "omit.txt")); //Place where all names that are located in the omit file are stored

        //Loads them
        for(int num=0; num<2; num++) {

            ArrayList<String> jsonStrings;

            //Get all the assets from the mod and the default assets
            if(!parsingPack) {
                num++; Display.displayPackMessage("Loading achievements from the default pack.");
                jsonStrings = getJsonFilesAsString(directory.getPath().replace("\\", "/"));
            } else {
                Display.displayPackMessage("Loading from modpack");
                jsonStrings = new ArrayList<>();
                for (String s : Objects.requireNonNull(directory.list())) {
                    if(!s.endsWith(".json")) { continue; } //This is not a file we want to look at (an omit file or a directory)
                    try (Scanner scan = new Scanner(new File(directory + File.separator + s)).useDelimiter("\\Z")) {
                            jsonStrings.add(scan.next());
                    } catch(NoSuchElementException ignore) {
                    } catch (IOException | NullPointerException e) {
                        e.printStackTrace();
                        Display.displayError("Error while reading file '" + s + "' from modpack.");
                    }
                }
            }

            for(String f : jsonStrings) {
                JSONObject achievementFile;
                try { achievementFile = (JSONObject) parser.parse(f); } catch (ParseException e) { Display.displayPackError("Having trouble parsing from file '" + f + "'"); e.printStackTrace(); continue; }
                if(achievementFile == null) { continue; }
                String name = (String)achievementFile.get("name");
                if(usedNames.contains(name) || (!parsingPack && namesToBeOmitted.contains(name))) { Display.displayPackError("An achievement does not have a name"); Display.changePackTabbing(false); continue; }
                String description = (String)achievementFile.get("description");
                Display.displayPackMessage("Loading achievement '" + name + "'");
                Display.changePackTabbing(true);
                achievements.add(new Achievement(name, description, loadMethods(Requirement.class, (JSONArray)achievementFile.get("requirements"), Achievement.class), loadMethods(Reward.class, (JSONArray)achievementFile.get("rewards"), Achievement.class)));
                Display.changePackTabbing(false);
                usedNames.add(name);
            }
            directory=achievementDir;
            parsingPack=false;
            if(namesToBeOmitted.contains("%all%")) { break; }
        }
        Display.displayProgressMessage("Achievements loaded.");
        Display.changePackTabbing(false);
        return true;
    }

    /**
     * Loads the items.
     * <p>First loads from the pack (If one is specified in the config file), then loads from the default pack.
     * @return      True if successful, false if unsuccessful.
     */
    public static boolean loadItems() {
        parsingPack = false;
        Display.displayProgressMessage("Loading the items...");
        Display.changePackTabbing(true);
        //if(!itemDir.exists()) { Display.displayError("Could not find the default items directory."); Display.changePackTabbing(false); return false;}
        File directory = itemDir;

        //Determine if there is a pack to be loaded and start loading from it if there is
        File packDirectory = getPackDirectory("items", packUsed);
        if(packDirectory != null) { directory = packDirectory; parsingPack = true;}

        boolean modSetFists = false;

        ArrayList<String> namesToBeOmitted = getOmittedAssets(new File(directory + File.separator + "omit.txt")); //Place where all names that are located in the omit file are stored
        for(int num=0; num<2; num++) {

            Display.changePackTabbing(true);
            //Determine if there is a pack to be loaded and start loading from it if there is
            if(!parsingPack || getPackDirectory("weapons", directory) != null) {
                Display.displayPackMessage("Loading weapons");
                ArrayList<String> usedNames = new ArrayList<>();

                ArrayList<String> jsonStrings;

                //Get all the assets from the mod and the default assets
                if(!parsingPack) {
                    num++; Display.displayPackMessage("Loading weapons from the default pack.");
                    jsonStrings = getJsonFilesAsString(directory.getPath().replace("\\", "/") + "/weapons");
                } else {
                    Display.displayPackMessage("Loading from modpack");
                    jsonStrings = new ArrayList<>();
                    File itemDir = new File(directory.getPath() + File.separator + "weapons");
                    if(itemDir.exists()) {
                        for (String s : Objects.requireNonNull(directory.list())) {
                            if(!s.endsWith(".json")) { continue; } //This is not a file we want to look at (an omit file or a directory)
                            try (Scanner scan = new Scanner(new File(directory + File.separator + s)).useDelimiter("\\Z")) {
                                jsonStrings.add(scan.next());
                            } catch(NoSuchElementException e) {
                            } catch (IOException | NullPointerException e) {
                                e.printStackTrace();
                                Display.displayError("Error while reading file '" + s + "' from modpack.");
                            }
                        }
                    }
                }

                for(String f : jsonStrings) {
                    Display.changePackTabbing(true);
                    JSONObject itemFile;
                    try{ itemFile = (JSONObject) parser.parse(f); } catch(ParseException e) { Display.displayError("Having problems with parsing weapon in file '" + f + "'"); continue; }
                    if(itemFile == null) { continue; }
                    String name = Weapon.defaultName;                           if(itemFile.get("name") != null) { name = (String)itemFile.get("name");}
                    if(usedNames.contains(name) || (!parsingPack && namesToBeOmitted.contains(name))) { Display.changePackTabbing(false); continue; }
                    Display.displayPackMessage("Loading item '" + name + "' of type 'weapon'");
                    if(name.equalsIgnoreCase("fists")) { modSetFists = true; }
                    int damage = Weapon.defaultDamage;                          if(itemFile.get("damage") != null) { damage = Integer.parseInt((String)itemFile.get("damage")); }
                    int critChance = Weapon.defaultCritChance;                  if(itemFile.get("critchance") != null) { critChance = Integer.parseInt((String)itemFile.get("critchance")); }
                    int missChance = Weapon.defaultMissChance;                  if(itemFile.get("misschance") != null) { missChance = Integer.parseInt((String)itemFile.get("misschance")); }
                    String description = Weapon.defaultDescription;             if(itemFile.get("description") != null) { description = (String)itemFile.get("description"); }
                    int durability = Weapon.defaultDurability;                  if(itemFile.get("durability") != null) { durability = Integer.parseInt((String)itemFile.get("durability")); }
                    int maxDurability = Weapon.defaultMaxDurability;            if(itemFile.get("maxDurability") != null) { maxDurability = Integer.parseInt((String)itemFile.get("maxDurability")); }
                    boolean unbreakable = Weapon.defaultUnbreakable;            if(itemFile.get("unbreakable") != null) { unbreakable = Boolean.parseBoolean((String)itemFile.get("unbreakable")); }
                    ArrayList<CustomVariable> customVars = new ArrayList<>();
                    for (CustomVariable c : weaponCustomVariables) {
                        if (c.getName().equalsIgnoreCase("fists") && c.getName().equalsIgnoreCase("unbreakable")) { //The fists cannot break no matter what
                            c.setValue(false);
                            if (parsingPack) {
                                modSetFists = true;
                            }
                            continue;
                        }
                        if (itemFile.get(c.getName()) != null) {
                            CustomVariable cv = new CustomVariable(c.getName(), c.getValue(), c.getValueType(), c.getInOutput(), c.getIsSaved());
                            try {
                                //The syntax for using a null value is "%null%"
                                if ((itemFile.get(c.getName())).equals("%null%")) {
                                    cv.setValue(null);
                                } else if (cv.getValueType().equals(int.class)) {
                                    cv.setValue(Integer.parseInt((String) itemFile.get(c.getName())));
                                } else if (cv.getValueType().equals(String.class)) {
                                    cv.setValue(itemFile.get(c.getName()));
                                } else if (cv.getValueType().equals(double.class)) {
                                    cv.setValue(Double.parseDouble((String) itemFile.get(c.getName())));
                                } else if (cv.getValueType().equals(boolean.class)) {
                                    cv.setValue(Boolean.parseBoolean((String) itemFile.get(c.getName())));
                                } else if (cv.getValueType().equals(Class.class)) {
                                    try {
                                        cv.setValue(Class.forName((String) itemFile.get(c.getName())));
                                    } catch (ClassNotFoundException e) {
                                        Display.changePackTabbing(false);
                                        continue;
                                    }
                                }
                                customVars.add(cv);
                            } catch (Exception e) {
                                e.printStackTrace();
                                customVars.add(c);
                            }
                        } else {
                            customVars.add(c);
                        }
                    }
                    //Add it
                    weapons.add(new Weapon(name, description, damage, critChance, missChance, customVars, durability,  maxDurability, unbreakable));
                    usedNames.add(name);
                    Display.changePackTabbing(false);
                }
                //Add the fists weapon, which always exists (not if the mod already added it
                if(!modSetFists) { //Note that the vanilla textfighter mod also sets the fists
                    weapons.add(new Weapon("fists", "Your fists, you don't need a description about what that is.", 5, 10, 5, new ArrayList<>(), 100, 100, true));
                }
            }
            if(!parsingPack || getPackDirectory("armor", directory) != null) {
                Display.displayPackMessage("Loading armor");
                ArrayList<String> usedNames = new ArrayList<>();

                ArrayList<String> jsonStrings;

                //Get all the assets from the mod and the default assets
                if(!parsingPack) {
                    num++; Display.displayPackMessage("Loading armor from the default pack.");
                    jsonStrings = getJsonFilesAsString(directory.getPath().replace("\\", "/") + "/armor");
                } else {
                    Display.displayPackMessage("Loading from modpack");
                    jsonStrings = new ArrayList<>();
                    File itemDir = new File(directory.getPath() + File.separator + "armor");
                    if(itemDir.exists()) {
                        for (String s : Objects.requireNonNull(directory.list())) {
                            if(!s.endsWith(".json")) { continue; } //This is not a file we want to look at (an omit file or a directory)
                            try {
                                Scanner scan = new Scanner(new File(directory + File.separator + s)).useDelimiter("\\Z");
                                jsonStrings.add(scan.next());
                                scan.close();
                            } catch (IOException | NullPointerException e) {
                                e.printStackTrace();
                                Display.displayError("Error while reading file '" + s + "' from modpack.");
                            }
                        }
                    }
                }

                for(String f : jsonStrings) {
                    Display.changePackTabbing(true);
                    JSONObject itemFile;
                    try{ itemFile = (JSONObject) parser.parse(f); } catch(ParseException e) { Display.displayError("Having problems with parsing armor in file '" + f + "'"); continue;}
                    if(itemFile == null) { continue; }
                    String name = Armor.defaultName;                            if(itemFile.get("name") != null) { name = (String)itemFile.get("name");}
                    if(usedNames.contains(name) || (!parsingPack && namesToBeOmitted.contains(name))) { Display.changePackTabbing(false); continue; }
                    Display.displayPackMessage("Loading item '" + name + "' of type 'armor'");
                    double protectionamount = Armor.defaultProtectionAmount;    if(itemFile.get("protectionamount") != null) { protectionamount = Double.parseDouble((String)itemFile.get("protectionamount")); }
                    String description = Armor.defaultDescription;              if(itemFile.get("description") != null) { description = (String)itemFile.get("description"); }
                    int durability = Armor.defaultDurability;                  if(itemFile.get("durability") != null) { durability = Integer.parseInt((String)itemFile.get("durability")); }
                    int maxDurability = Armor.defaultMaxDurability;            if(itemFile.get("maxDurability") != null) { maxDurability = Integer.parseInt((String)itemFile.get("maxDurability")); }
                    boolean unbreakable = Armor.defaultUnbreakable;            if(itemFile.get("unbreakable") != null) { unbreakable = Boolean.parseBoolean((String)itemFile.get("unbreakable")); }
                    ArrayList<CustomVariable> customVars = new ArrayList<>();
                    for (CustomVariable c : armorCustomVariables) {
                        if (itemFile.get(c.getName()) != null) {
                            CustomVariable cv = new CustomVariable(c.getName(), c.getValue(), c.getValueType(), c.getInOutput(), c.getIsSaved());
                            try {
                                //The syntax for using a null value is "%null%"
                                if ((itemFile.get(cv.getName())).equals("%null%")) {
                                    cv.setValue(null);
                                } else if (cv.getValueType().equals(int.class)) {
                                    cv.setValue(Integer.parseInt((String) itemFile.get(cv.getName())));
                                } else if (cv.getValueType().equals(String.class)) {
                                    cv.setValue(itemFile.get(cv.getName()));
                                } else if (cv.getValueType().equals(double.class)) {
                                    cv.setValue(Double.parseDouble((String) itemFile.get(cv.getName())));
                                } else if (cv.getValueType().equals(boolean.class)) {
                                    cv.setValue(Boolean.parseBoolean((String) itemFile.get(cv.getName())));
                                } else if (cv.getValueType().equals(Class.class)) {
                                    try {
                                        cv.setValue(Class.forName((String) itemFile.get(cv.getName())));
                                    } catch (ClassNotFoundException e) {
                                        Display.changePackTabbing(false);
                                        continue;
                                    }
                                }
                                customVars.add(cv);
                            } catch (Exception e) {
                                customVars.add(c);
                            }
                        } else {
                            customVars.add(c);
                        }
                    }
                    //Add it
                    armors.add(new Armor(name, description, protectionamount, durability, maxDurability, unbreakable, customVars));
                    usedNames.add(name);
                    Display.changePackTabbing(false);
                }
            }
            if(!parsingPack || getPackDirectory("tools", directory) != null) {
                Display.displayPackMessage("Loading tools");
                ArrayList<String> usedNames = new ArrayList<>();

                ArrayList<String> jsonStrings;

                //Get all the assets from the mod and the default assets
                if(!parsingPack) {
                    num++; Display.displayPackMessage("Loading tools from the default pack.");
                    jsonStrings = getJsonFilesAsString(directory.getPath().replace("\\", "/") + "/tools");
                } else {
                    Display.displayPackMessage("Loading from modpack");
                    jsonStrings = new ArrayList<>();
                    File itemDir = new File(directory.getPath() + File.separator + "tools");
                    if(itemDir.exists()) {
                        for (String s : Objects.requireNonNull(directory.list())) {
                            if(!s.endsWith(".json")) { continue; } //This is not a file we want to look at (an omit file or a directory)
                            try {
                                Scanner scan = new Scanner(new File(directory + File.separator + s)).useDelimiter("\\Z");
                                jsonStrings.add(scan.next());
                                scan.close();
                            } catch (IOException | NullPointerException e) {
                                e.printStackTrace();
                                Display.displayError("Error while reading file '" + s + "' from modpack.");
                            }
                        }
                    }
                }

                for(String f : jsonStrings) {
                    Display.changePackTabbing(true);
                    JSONObject itemFile;
                    try{ itemFile = (JSONObject) parser.parse(f);  } catch(ParseException e) { Display.displayError("Having problems with parsing tool in file '" + f + "'"); continue; }
                    if(itemFile == null) { continue; }
                    String name = Tool.defaultName; if(itemFile.get("name") != null) { name = (String)itemFile.get("name");}
                    if(usedNames.contains(name) || (!parsingPack && namesToBeOmitted.contains(name))) { Display.changePackTabbing(false); continue; }
                    Display.displayPackMessage("Loading item '" + name + "' of type 'tool'");
                    String description = Tool.defaultDescription;               if(itemFile.get("description") != null) { description = (String)itemFile.get("description"); }
                    int durability = Tool.defaultDurability;                    if(itemFile.get("durability") != null) { durability = Integer.parseInt((String)itemFile.get("durability")); }
                    int maxDurability = Tool.defaultMaxDurability;            if(itemFile.get("maxDurability") != null) { maxDurability = Integer.parseInt((String)itemFile.get("maxDurability")); }
                    boolean unbreakable = Tool.defaultUnbreakable;              if(itemFile.get("unbreakable") != null) { unbreakable = Boolean.parseBoolean((String)itemFile.get("unbreakable")); }
                    ArrayList<CustomVariable> customVars = new ArrayList<>();
                    for (CustomVariable c : toolCustomVariables) {
                        if (itemFile.get(c.getName()) != null) {
                            CustomVariable cv = new CustomVariable(c.getName(), c.getValue(), c.getValueType(), c.getInOutput(), c.getIsSaved());
                            try {
                                //The syntax for using a null value is "%null%"
                                if ((itemFile.get(cv.getName())).equals("%null%")) {
                                    cv.setValue(null);
                                } else if (cv.getValueType().equals(int.class)) {
                                    cv.setValue(Integer.parseInt((String) itemFile.get(cv.getName())));
                                } else if (cv.getValueType().equals(String.class)) {
                                    cv.setValue(itemFile.get(cv.getName()));
                                } else if (cv.getValueType().equals(double.class)) {
                                    cv.setValue(Double.parseDouble((String) itemFile.get(cv.getName())));
                                } else if (cv.getValueType().equals(boolean.class)) {
                                    cv.setValue(Boolean.parseBoolean((String) itemFile.get(cv.getName())));
                                } else if (cv.getValueType().equals(Class.class)) {
                                    try {
                                        cv.setValue(Class.forName((String) itemFile.get(cv.getName())));
                                    } catch (ClassNotFoundException e) {
                                        Display.changePackTabbing(false);
                                        continue;
                                    }
                                }
                                customVars.add(cv);
                            } catch (Exception e) {
                                customVars.add(c);
                            }
                        } else {
                            customVars.add(c);
                        }
                    }
                    //Add it
                    tools.add(new Tool(name, description, customVars, durability, maxDurability, unbreakable));
                    usedNames.add(name);
                    Display.changePackTabbing(false);
                }
            }
            if(!parsingPack || getPackDirectory("specialitems", directory) != null) {
                Display.displayPackMessage("Loading specialitems");
                ArrayList<String> usedNames = new ArrayList<>();

                ArrayList<String> jsonStrings;

                //Get all the assets from the mod and the default assets
                if(!parsingPack) {
                    num++; Display.displayPackMessage("Loading specialitems from the default pack.");
                    jsonStrings = getJsonFilesAsString(directory.getPath().replace("\\", "/") + "/specialitems");
                } else {
                    Display.displayPackMessage("Loading from modpack");
                    jsonStrings = new ArrayList<>();
                    File itemDir = new File(directory.getPath() + File.separator + "specialitems");
                    if(itemDir.exists()) {
                        for (String s : Objects.requireNonNull(directory.list())) {
                            if(!s.endsWith(".json")) { continue; } //This is not a file we want to look at (an omit file or a directory)s
                            try {
                                Scanner scan = new Scanner(new File(directory + File.separator + s)).useDelimiter("\\Z");
                                jsonStrings.add(scan.next());
                                scan.close();
                            } catch (IOException | NullPointerException e) {
                                e.printStackTrace();
                                Display.displayError("Error while reading file '" + s + "' from modpack.");
                            }
                        }
                    }
                }

                for(String f : jsonStrings) {
                    Display.changePackTabbing(true);
                    JSONObject itemFile;
                    try{ itemFile = (JSONObject) parser.parse(f); } catch(ParseException e) { Display.displayError("Having problems with parsing special item in file '" + f + "'"); continue; }
                    if(itemFile == null) { continue; }
                    String name = SpecialItem.defaultName;                      if(itemFile.get("name") != null) { name = (String)itemFile.get("name");}
                    if(usedNames.contains(name) || (!parsingPack && namesToBeOmitted.contains(name))) { Display.changePackTabbing(false); continue; }
                    Display.displayPackMessage("Loading item '" + name + "' of type 'specialitem'");
                    String description = SpecialItem.defaultDescription;        if(itemFile.get("description") != null) { description = (String)itemFile.get("description"); }
                    ArrayList<CustomVariable> customVars = new ArrayList<>();
                    for (CustomVariable c : specialitemCustomVariables) {
                        if (itemFile.get(c.getName()) != null) {
                            CustomVariable cv = new CustomVariable(c.getName(), c.getValue(), c.getValueType(), c.getInOutput(), c.getIsSaved());
                            try {
                                //The syntax for using a null value is "%null%"
                                if ((itemFile.get(cv.getName())).equals("%null%")) {
                                    cv.setValue(null);
                                } else if (cv.getValueType().equals(int.class)) {
                                    cv.setValue(Integer.parseInt((String) itemFile.get(cv.getName())));
                                } else if (cv.getValueType().equals(String.class)) {
                                    cv.setValue(itemFile.get(cv.getName()));
                                } else if (cv.getValueType().equals(double.class)) {
                                    cv.setValue(Double.parseDouble((String) itemFile.get(cv.getName())));
                                } else if (cv.getValueType().equals(boolean.class)) {
                                    cv.setValue(Boolean.parseBoolean((String) itemFile.get(cv.getName())));
                                } else if (cv.getValueType().equals(Class.class)) {
                                    try {
                                        cv.setValue(Class.forName((String) itemFile.get(cv.getName())));
                                    } catch (ClassNotFoundException e) {
                                        Display.changePackTabbing(false);
                                        continue;
                                    }
                                }
                                customVars.add(cv);
                            } catch (Exception e) {
                                customVars.add(c);
                            }
                        } else {
                            customVars.add(c);
                        }
                    }
                    //Add it
                    specialItems.add(new SpecialItem(name, description, customVars));
                    usedNames.add(name);
                    Display.changePackTabbing(false);
                }
            }
            directory=enemyDir;
            parsingPack=false;
            Display.changePackTabbing(false);
            if(namesToBeOmitted.contains("%all%")) { break; }
        }
        Display.changePackTabbing(false);
        Display.displayProgressMessage("Items loaded.");
        return true;
    }

    /**
     * Loads the custom variables.
     * <p>First loads from the pack (If one is specified in the config file), then loads from the default pack.
     * @return      Whether or not successful.
     */
    public static boolean loadCustomVariables() {
        parsingPack = false;
        Display.displayProgressMessage("Loading the custom variables...");
        Display.changePackTabbing(true);
        ArrayList<String> toolsUsedNames = new ArrayList<>();
        ArrayList<String> armorUsedNames = new ArrayList<>();
        ArrayList<String> weaponsUsedNames = new ArrayList<>();
        ArrayList<String> specialItemsUsedNames = new ArrayList<>();
        ArrayList<String> playerUsedNames = new ArrayList<>();
        ArrayList<String> enemiesUsedNames = new ArrayList<>();
        File directory = customVariablesDir;

        //Determine if there is a pack to be loaded and start loading from it if there is
        File packDirectory = getPackDirectory("customvariables", packUsed);
        if(packDirectory != null) { directory = packDirectory; parsingPack = true;}

        ArrayList<String> filesToBeOmitted = getOmittedAssets(new File(packDirectory + File.separator + "omit.txt"));

        //Load them
        for(int num=0; num<2; num++) {

            ArrayList<String> jsonStrings;

            //Get all the assets from the mod and the default assets
            if(!parsingPack) {
                num++;
                Display.displayPackMessage("Loading custom variables from the default pack");
                jsonStrings = getJsonFilesAsString(directory.getPath().replace("\\", "/"));
            } else {
                Display.displayPackMessage("Loading from modpack");
                jsonStrings = new ArrayList<>();
                for (String s : Objects.requireNonNull(directory.list())) {
                    if(!s.endsWith(".json")) { continue; } //This is not a file we want to look at (an omit file or a directory)
                    try (Scanner scan = new Scanner(new File(directory + File.separator + s)).useDelimiter("\\Z")) {
                        jsonStrings.add(scan.next());
                    } catch(NoSuchElementException e) {
                    } catch (IOException | NullPointerException e) {
                        e.printStackTrace();
                        Display.displayError("Error while reading file '" + s + "' from modpack.");
                    }
                }
            }

            Display.changePackTabbing(true);
            for(String s : jsonStrings) { //Parse each file
                if(filesToBeOmitted.contains(s)) { continue; }
                ArrayList<CustomVariable> variables = new ArrayList<>();
                JSONObject itemFile;
                try { itemFile = (JSONObject)parser.parse(s); } catch(ParseException e) { e.printStackTrace(); Display.displayPackError("Having trouble parsing custom variables from file '" + s + "'"); continue; }
                String owner = (String)itemFile.get("owner"); if(owner == null) { Display.displayError("This customvariables file has no owner type. Omitting..."); continue; }
                JSONArray valuesArray = null; if(itemFile.get("values") != null) { valuesArray = (JSONArray)itemFile.get("values"); }
                if(valuesArray != null) {
                    for (Object o : valuesArray) {
                        JSONObject obj = (JSONObject) o;
                        String name;
                        if (obj.get("name") != null) {
                            name = (String) obj.get("name");
                            Display.displayPackMessage("Loading custom variable '" + name + "'.");
                        } else {
                            Display.displayPackError("This custom variable does not have a name. Omitting...");
                            continue;
                        }
                        switch (s) {
                            case "player.json":
                                if (playerUsedNames.contains(name)) {
                                    Display.displayPackError("Found duplicate custom variable '" + name + "'");
                                    continue;
                                }
                                break;
                            case "enemy.json":
                                if (enemiesUsedNames.contains(name)) {
                                    Display.displayPackError("Found duplicate custom variable '" + name + "'");
                                    continue;
                                }
                                break;
                            case "weapon.json":
                                if (weaponsUsedNames.contains(name)) {
                                    Display.displayPackError("Found duplicate custom variable '" + name + "'");
                                    continue;
                                }
                                break;
                            case "armor.json":
                                if (armorUsedNames.contains(name)) {
                                    Display.displayPackError("Found duplicate custom variable '" + name + "'");
                                    continue;
                                }
                                break;
                            case "specialitem.json":
                                if (specialItemsUsedNames.contains(name)) {
                                    Display.displayPackError("Found duplicate custom variable '" + name + "'");
                                    continue;
                                }
                                break;
                            case "tool.json":
                                if (toolsUsedNames.contains(name)) {
                                    Display.displayPackError("Found duplicate custom variable '" + name + "'");
                                    continue;
                                }
                                break;
                        }
                        Object value;
                        if (obj.get("value") != null) {
                            value = obj.get("value");
                        } else {
                            Display.displayPackError("This custom variable does not have a value. Omitting...");
                            continue;
                        }
                        Class type = null;
                        int typeRaw;
                        if (obj.get("type") != null) {
                            typeRaw = Integer.parseInt((String) obj.get("type"));
                        } else {
                            Display.displayPackError("This custom variable does not have a type. Omitting...");
                            continue;
                        }
                        boolean isSaved = false;
                        if (obj.get("isSaved") != null) {
                            isSaved = Boolean.parseBoolean((String) obj.get("isSaved"));
                        }
                        //Cast the values to the correct type
                        if (value.equals("%null%")) {
                            value = null;
                        } else if (typeRaw == 0) {
                            if (value.equals("%null%")) {
                                value = null;
                            }
                            type = String.class;
                        } else if (typeRaw == 1) {
                            value = Integer.parseInt((String) value);
                            type = int.class;
                        } else if (typeRaw == 2) {
                            value = Double.parseDouble((String) value);
                            type = double.class;
                        } else if (typeRaw == 3) {
                            value = Boolean.parseBoolean((String) value);
                            type = boolean.class;
                        } else if (typeRaw == 4) {
                            try {
                                value = Class.forName((String) value);
                            } catch (ClassNotFoundException e) {
                                Display.displayPackError("The custom variable tried to get a class that does not exist. Omitting... ");
                                Display.changePackTabbing(false);
                                continue;
                            }
                            type = Class.class;
                        } else {
                            Display.displayPackError("This method has arguments that are not String, int, double, boolean, or class. Omitting...");
                            continue;
                        }
                        //InOuptut is true by default
                        boolean inOutput = true;
                        if (obj.get("inoutput") != null) {
                            inOutput = Boolean.parseBoolean((String) obj.get("inoutput"));
                        }
                        variables.add(new CustomVariable(name, value, type, inOutput, isSaved));
                        switch (owner) {
                            case "player.json":
                                playerUsedNames.add(name);
                                break;
                            case "enemy.json":
                                enemiesUsedNames.add(name);
                                break;
                            case "weapon.json":
                                weaponsUsedNames.add(name);
                                break;
                            case "armor.json":
                                armorUsedNames.add(name);
                                break;
                            case "specialitem.json":
                                specialItemsUsedNames.add(name);
                                break;
                            case "tool.json":
                                toolsUsedNames.add(name);
                                break;
                        }
                    }
                }
                //Add the customVariable to the correct arraylist
                switch (owner) {
                    case "player":
                        playerCustomVariables = variables;
                        break;
                    case "enemy":
                        enemyCustomVariables = variables;
                        break;
                    case "weapon":
                        weaponCustomVariables = variables;
                        break;
                    case "armor":
                        armorCustomVariables = variables;
                        break;
                    case "specialitem":
                        specialitemCustomVariables = variables;
                        break;
                    case "tool":
                        toolCustomVariables = variables;
                        break;
                }
            }
            parsingPack = false;
            Display.changePackTabbing(false);
            Display.changePackTabbing(false);
            Display.displayProgressMessage("Custom variables loaded");
            directory = locationDir;
            parsingPack = false;
            if(filesToBeOmitted.contains("%all%")) { break; }
        }
        return true;
    }

    /**
     * Loads the configured colors.
     * <p>If the config directory does not exist, then a new one is created</p>
     */
    public static void loadConfig() {
        Display.displayProgressMessage("Loading config...");
        if(configDir.exists()) {
            //Get the pack specified in the pack file
            try (BufferedReader br = new BufferedReader(new FileReader(packFile))) {
                String line = br.readLine();
                if(line != null && packDir != null && packDir.exists() && packDir.list() != null) {
                    for(String s : Objects.requireNonNull(packDir.list())){
                        if(s.equals(line)) {
                            packUsed = new File(packDir.getPath() + File.separator + line);
                            Display.displayProgressMessage("Current pack: " + line);
                            modName = line;
                        }
                    }
                }
            } catch(IOException e) { e.printStackTrace(); Display.displayWarning("Could not read the config file that defines desired pack!"); }
            Display.loadDesiredColors();
            Display.displayProgressMessage("Config loaded.");
        } else {
            Display.displayWarning("The config directory could not be found!\n Creating new config directory.");
            if(!configDir.mkdirs()) { Display.displayWarning("Unable to create a new config directory"); }
        }
        configLoaded = true;
    }

    /**
     * Loads the default values.
     * <p>NOTE: This one is different from the rest (Except custom variables): Loads from the default pack or the custom pack NOT BOTH.</p>
     * @return      Whether or not successful.
     */
    public static boolean loadDefaultValues() {
        parsingPack = false;
        //Custom parsing tags are located in interface packs
        Display.displayProgressMessage("Loading the default player/enemy/item values...");
        Display.changePackTabbing(true);
        File directory = defaultValuesDirectory;

        //Determine if there is a pack to be loaded and start loading from it if there is
        File packDirectory = getPackDirectory("defaultvalues", packUsed);
        if(packDirectory != null) { directory = packDirectory; parsingPack = true;}

        //ArrayList<String> filesToBeOmitted = getOmittedAssets(new File(packDirectory + File.separator + "omit.txt"));

        //Load them
        Display.changePackTabbing(true);
        for(int num=0; num<2; num++) {
            if(!parsingPack) {
                num++;
                Display.displayPackMessage("Loading default values from the default pack");
            } else {
                Display.displayPackMessage("Loading default values from the modpack");
            }

            //I know this is bad style, but the alternative way the code looks is just... please no.
            if (true) {
                Display.displayPackMessage("Loading player values...");

                Display.changePackTabbing(true);
                try {
                    String jsonString = "{}";
                    if (!parsingPack) {
                        Display.displayPackMessage("Loading player values from the default pack");
                        jsonString = getSingleJsonFileAsString(directory.getPath().replace("\\", "/") + "/player.json");
                    } else {
                        Display.displayPackMessage("Loading from modpack");
                        try (Scanner scan = new Scanner(new File(directory + File.separator + "player.json")).useDelimiter("\\Z")) {
                            jsonString = scan.next();
                        } catch(NoSuchElementException e) {
                            jsonString = "{}";
                        } catch (IOException | NullPointerException e) {
                            e.printStackTrace();
                            Display.displayError("Error while reading file 'player.json' from modpack.");
                        }
                    }
                    JSONObject valuesFile = (JSONObject) parser.parse(jsonString);
                    //Player values
                    if (valuesFile.get("coins") != null) {
                        Player.defaultCoins = Integer.parseInt((String) valuesFile.get("coins"));
                    }
                    if (valuesFile.get("magic") != null) {
                        Player.defaultMagic = Integer.parseInt((String) valuesFile.get("magic"));
                    }
                    if (valuesFile.get("metalscraps") != null) {
                        Player.defaultMetalscraps = Integer.parseInt((String) valuesFile.get("metalscraps"));
                    }
                    if (valuesFile.get("health") != null) {
                        Player.defaulthp = Integer.parseInt((String) valuesFile.get("health"));
                    }
                    if (valuesFile.get("maxhp") != null) {
                        Player.defaultMaxhp = Integer.parseInt((String) valuesFile.get("maxhp"));
                    }
                    if (valuesFile.get("totalProtection") != null) {
                        Player.defaultTotalProtection = Integer.parseInt((String) valuesFile.get("defaultTotalProtection"));
                    }
                    if (valuesFile.get("maxProtection") != null) {
                        Player.defaultMaxProtection = Integer.parseInt((String) valuesFile.get("defaultMaxProtection"));
                    }
                    if (valuesFile.get("healthPotions") != null) {
                        Player.defaultHealthPotions = Integer.parseInt((String) valuesFile.get("healthPotions"));
                    }
                    if (valuesFile.get("strengthPotions") != null) {
                        Player.defaultStrength = Integer.parseInt((String) valuesFile.get("strengthPotions"));
                    }
                    if (valuesFile.get("invincibilityPotions") != null) {
                        Player.defaultInvincibilityPotions = Integer.parseInt((String) valuesFile.get("invincibilityPotions"));
                    }
                    if (valuesFile.get("hpHealthPotionsGive") != null) {
                        Player.defaultHpHealthPotionsGive = Integer.parseInt((String) valuesFile.get("hpHealthPotionsGive"));
                    }
                    if (valuesFile.get("turnsStrengthPotionsGive") != null) {
                        Player.defaultTurnsStrengthPotionsGive = Integer.parseInt((String) valuesFile.get("turnsStrengthPotionsGive"));
                    }
                    if (valuesFile.get("turnsInvincibilityPotionsGive") != null) {
                        Player.defaultTurnsInvincibilityPotionsGive = Integer.parseInt((String) valuesFile.get("turnsInvincibilityPotionsGive"));
                    }
                    if (valuesFile.get("turnsWithStrengthLeft") != null) {
                        Player.defaultTurnsWithStrengthLeft = Integer.parseInt((String) valuesFile.get("turnsWithStrengthLeft"));
                    }
                    if (valuesFile.get("turnsWithInvincibilityLeft") != null) {
                        Player.defaultTurnsWithInvincibilityLeft = Integer.parseInt((String) valuesFile.get("turnsWithInvincibilityLeft"));
                    }
                    if (valuesFile.get("strengthPotionMultiplier") != null) {
                        Player.defaultStrengthPotionMultiplier = Integer.parseInt((String) valuesFile.get("strengthPotionMultiplier"));
                    }
                    Display.changePackTabbing(false);

                } catch (ParseException e) {
                    Display.changePackTabbing(false);
                }
            }
            if (true) {
                Display.displayPackMessage("Loading enemy values");

                Display.changePackTabbing(true);
                try {
                    String jsonString = "{}";
                    if (!parsingPack) {
                        Display.displayPackMessage("Loading enemy values from the default pack");
                        jsonString = getSingleJsonFileAsString(directory.getPath().replace("\\", "/") + "/enemy.json");
                    } else {
                        Display.displayPackMessage("Loading from modpack");
                        try {
                            Scanner scan = new Scanner(new File(directory + File.separator + "enemy.json")).useDelimiter("\\Z");
                            jsonString = scan.next();
                            scan.close();
                        } catch (IOException | NullPointerException e) {
                            e.printStackTrace();
                            Display.displayError("Error while reading file 'enemy.json' from modpack.");
                        }
                    }
                    JSONObject valuesFile = (JSONObject) parser.parse(jsonString);
                    //Enemy values
                    if (valuesFile.get("name") != null) {
                        Enemy.defaultName = (String) valuesFile.get("name");
                    }
                    if (valuesFile.get("health") != null) {
                        Enemy.defaultHp = Integer.parseInt((String) valuesFile.get("health"));
                    }
                    if (valuesFile.get("maxhp") != null) {
                        Enemy.defaultMaxhp = Integer.parseInt((String) valuesFile.get("maxhp"));
                    }
                    if (valuesFile.get("strength") != null) {
                        Enemy.defaultStrength = Integer.parseInt((String) valuesFile.get("strength"));
                    }
                    if (valuesFile.get("levelRequirement") != null) {
                        Enemy.defaultLevelRequirement = Integer.parseInt((String) valuesFile.get("levelRequirement"));
                    }
                    if (valuesFile.get("turnsWithInvincibilityLeft") != null) {
                        Enemy.defaultTurnsWithInvincibiltyLeft = Integer.parseInt((String) valuesFile.get("turnsWithInvisibilityLeft"));
                    }
                    Display.changePackTabbing(false);
                } catch (ParseException e) {
                    Display.changePackTabbing(false);
                }
            }
            if (true) {
                Display.displayPackMessage("Loading weapon values");
                Display.changePackTabbing(true);
                try {
                    String jsonString = "{}";
                    if (!parsingPack) {
                        Display.displayPackMessage("Loading weapon values from the default pack");
                        jsonString = getSingleJsonFileAsString(directory.getPath().replace("\\", "/") + "/weapon.json");
                    } else {
                        Display.displayPackMessage("Loading from modpack");
                        try {
                            Scanner scan = new Scanner(new File(directory + File.separator + "weapon.json")).useDelimiter("\\Z");
                            jsonString = scan.next();
                            scan.close();
                        } catch (IOException | NullPointerException e) {
                            e.printStackTrace();
                            Display.displayError("Error while reading file 'weapon.json' from modpack.");
                        }
                    }
                    JSONObject valuesFile = (JSONObject) parser.parse(jsonString);
                    //Weapon values
                    if (valuesFile.get("name") != null) {
                        Weapon.defaultName = (String) valuesFile.get("name");
                    }
                    if (valuesFile.get("description") != null) {
                        Weapon.defaultDescription = (String) valuesFile.get("description");
                    }
                    if (valuesFile.get("damage") != null) {
                        Weapon.defaultDamage = Integer.parseInt((String) valuesFile.get("damage"));
                    }
                    if (valuesFile.get("critChance") != null) {
                        Weapon.defaultCritChance = Integer.parseInt((String) valuesFile.get("critChance"));
                    }
                    if (valuesFile.get("missChance") != null) {
                        Weapon.defaultMissChance = Integer.parseInt((String) valuesFile.get("missChance"));
                    }
                    if (valuesFile.get("durability") != null) {
                        Weapon.defaultDurability = Integer.parseInt((String) valuesFile.get("durability"));
                    }
                    if (valuesFile.get("unbreakable") != null) {
                        Weapon.defaultUnbreakable = Boolean.parseBoolean((String) valuesFile.get("unbreakable"));
                    }
                    if (valuesFile.get("maxDurability") != null) {
                        Weapon.defaultMaxDurability = Integer.parseInt((String) valuesFile.get("maxDurability"));
                    }
                    Display.changePackTabbing(false);
                } catch (ParseException e) {
                    e.printStackTrace();
                    Display.changePackTabbing(false);
                }
            }
            if (true) {
                Display.displayPackMessage("Loading armor values");
                Display.changePackTabbing(true);
                try {
                    String jsonString = "{}";
                    if (!parsingPack) {
                        Display.displayPackMessage("Loading armor values from the default pack");
                        jsonString = getSingleJsonFileAsString(directory.getPath().replace("\\", "/") + "/armor.json");
                    } else {
                        Display.displayPackMessage("Loading from modpack");
                        try {
                            Scanner scan = new Scanner(new File(directory + File.separator + "armor.json")).useDelimiter("\\Z");
                            jsonString = scan.next();
                            scan.close();
                        } catch (IOException | NullPointerException e) {
                            e.printStackTrace();
                            Display.displayError("Error while reading file 'armor.json' from modpack.");
                        }
                    }
                    JSONObject valuesFile = (JSONObject) parser.parse(jsonString);
                    //Armor values
                    if (valuesFile.get("name") != null) {
                        Armor.defaultName = (String) valuesFile.get("name");
                    }
                    if (valuesFile.get("description") != null) {
                        Armor.defaultDescription = (String) valuesFile.get("description");
                    }
                    if (valuesFile.get("protectionAmount") != null) {
                        Armor.defaultName = (String) valuesFile.get("protectionAmount");
                    }
                    if (valuesFile.get("durability") != null) {
                        Weapon.defaultDurability = Integer.parseInt((String) valuesFile.get("durability"));
                    }
                    if (valuesFile.get("unbreakable") != null) {
                        Weapon.defaultUnbreakable = Boolean.parseBoolean((String) valuesFile.get("unbreakable"));
                    }
                    if (valuesFile.get("maxDurability") != null) {
                        Weapon.defaultMaxDurability = Integer.parseInt((String) valuesFile.get("maxDurability"));
                    }
                    Display.changePackTabbing(false);
                } catch (ParseException e) {
                    Display.changePackTabbing(false);
                }
            }
            if (true) {
                Display.displayPackMessage("Loading tool values");
                Display.changePackTabbing(true);
                try {
                    String jsonString = "{}";
                    if (!parsingPack) {
                        Display.displayPackMessage("Loading tool values from the default pack");
                        jsonString = getSingleJsonFileAsString(directory.getPath().replace("\\", "/") + "/tool.json");
                    } else {
                        Display.displayPackMessage("Loading from modpack");
                        try {
                            Scanner scan = new Scanner(new File(directory + File.separator + "tool.json")).useDelimiter("\\Z");
                            jsonString = scan.next();
                            scan.close();
                        } catch (IOException | NullPointerException e) {
                            e.printStackTrace();
                            Display.displayError("Error while reading file 'tool.json' from modpack.");
                        }
                    }
                    JSONObject valuesFile = (JSONObject) parser.parse(jsonString);
                    //Tool values
                    if (valuesFile.get("name") != null) {
                        Tool.defaultName = (String) valuesFile.get("name");
                    }
                    if (valuesFile.get("description") != null) {
                        Tool.defaultDescription = (String) valuesFile.get("description");
                    }
                    if (valuesFile.get("durability") != null) {
                        Tool.defaultDurability = Integer.parseInt((String) valuesFile.get("durability"));
                    }
                    if (valuesFile.get("unbreakable") != null) {
                        Tool.defaultUnbreakable = Boolean.parseBoolean((String) valuesFile.get("unbreakable"));
                    }
                    if (valuesFile.get("maxDurability") != null) {
                        Tool.defaultMaxDurability = Integer.parseInt((String) valuesFile.get("maxDurability"));
                    }
                    Display.changePackTabbing(false);
                } catch (ParseException e) {
                    Display.changePackTabbing(false);
                }
            }
            if (true) {
                Display.displayPackMessage("Loading specialitem values");
                Display.changePackTabbing(true);
                try {
                    String jsonString = "{}";
                    if (!parsingPack) {
                        Display.displayPackMessage("Loading specialitem values from the default pack");
                        jsonString = getSingleJsonFileAsString(directory.getPath().replace("\\", "/") + "/specialitem.json");
                    } else {
                        Display.displayPackMessage("Loading from modpack");
                        try {
                            Scanner scan = new Scanner(new File(directory + File.separator + "specialitem.json")).useDelimiter("\\Z");
                            jsonString = scan.next();
                            scan.close();
                        } catch (IOException | NullPointerException e) {
                            e.printStackTrace();
                            Display.displayError("Error while reading file 'specialitem.json' from modpack.");
                        }
                    }
                    JSONObject valuesFile = (JSONObject) parser.parse(jsonString);
                    //SpecialItem values
                    if (valuesFile.get("name") != null) {
                        SpecialItem.defaultName = (String) valuesFile.get("name");
                    }
                    if (valuesFile.get("description") != null) {
                        SpecialItem.defaultDescription = (String) valuesFile.get("description");
                    }
                    Display.changePackTabbing(false);
                } catch (ParseException e) {
                    Display.changePackTabbing(false);
                }
            }
            parsingPack = true;
            directory = defaultValuesDirectory;
        }
        parsingPack = false;
        Display.changePackTabbing(false);
        Display.changePackTabbing(false);
        Display.displayPackMessage("Player/enemy/item values loaded.");
        return true;
    }

    /**
     * Loads the death methods (methods that are invoked when the player dies).
     * <p>First loads from the pack (If one is specified in the config file), then loads from the default pack.
     * @return      Whether or not successful
     */
    public static boolean loadDeathMethods() {
        parsingPack = false;
        Display.displayProgressMessage("Loading the death methods...");
        Display.changePackTabbing(true);
        ArrayList<String> usedIds = new ArrayList<>();
        File file = deathmethodsFile;

        //Determine if there is a pack to be loaded and start loading from it if there is
        File packDirectory = getPackDirectory("deathmethods", packUsed);
        if(packDirectory != null && packDirectory.list() != null) {
            File newFile = new File(packDirectory.getPath() + File.separator + "deathmethods.json");
            if(newFile.exists()) { file = newFile; parsingPack = true; }
        }

        ArrayList<String> idsToBeOmitted = getOmittedAssets(new File(packDirectory + File.separator + "omit.txt")); //Place where all ids that are located in the omit file are stored
        //Loads them
        for(int num=0; num<2; num++) {

            String jsonString = "{}";

            //Get all the assets from the mod and the default assets
            if(!parsingPack) {
                num++;
                Display.displayPackMessage("Loading deathmethods from the default pack");
                jsonString = getSingleJsonFileAsString(deathmethodsFile.getPath().replace("\\", "/"));
                if(jsonString.isEmpty()) { jsonString = "{}"; }
            } else {
                Display.displayPackMessage("Loading from modpack");
                try (Scanner scan = new Scanner(file.getAbsolutePath())) {
                    jsonString = scan.next();
                } catch(NoSuchElementException e) {
                    jsonString = "{}";
                } catch (NullPointerException e) {
                    Display.displayError("Error while reading file '" + file.getAbsolutePath() + "' from modpack.");
                }
            }
            Display.changePackTabbing(true);
            JSONArray deathmethodFile;
            try { deathmethodFile = (JSONArray)(((JSONObject)parser.parse(jsonString)).get("deathmethods")); } catch ( ParseException e) { Display.displayPackError("Having trouble parsing from file '" + file.getName() + "'"); e.printStackTrace(); continue; }
            if(deathmethodFile == null) { continue; }
            for (Object o : deathmethodFile) {
                JSONObject obj = (JSONObject) o;
                String id = (String) obj.get("id");
                if (id == null) {
                    Display.displayPackError("A deathmethod to does not have an id. Omitting...");
                }
                if ((!parsingPack && idsToBeOmitted.contains(id)) || usedIds.contains(id)) {
                    continue;
                }
                Display.displayPackMessage("Loading deathmethod '" + id + "'");
                Display.changePackTabbing(true);
                TFMethod method = (TFMethod) loadMethod(TFMethod.class, obj, IDMethod.class);
                if (method != null) {
                    deathMethods.add(new IDMethod(method, id));
                } else {
                    Display.displayPackMessage("This death method does not have a valid method");
                }
                Display.changePackTabbing(false);
                usedIds.add(id);
            }
            Display.changePackTabbing(false);
            file=deathmethodsFile;
            parsingPack=false;
            if(idsToBeOmitted.contains("%all%")) { break; }
        }
        parsingPack = false;
        Display.displayProgressMessage("Death methods loaded.");
        Display.changePackTabbing(false);
        return true;
    }

    /**
     * Loads the death methods (methods that are invoked when the player dies).
     * <p>First loads from the pack (If one is specified in the config file), then loads from the default pack.
     * @return      Whether or not successful
     */
    public static boolean loadLevelUpMethods() {
        parsingPack = false;
        Display.displayProgressMessage("Loading the level up methods...");
        Display.changePackTabbing(true);
        ArrayList<String> usedIds = new ArrayList<>();
        File file = levelupmethodsFile;

        //Determine if there is a pack to be loaded and start loading from it if there is
        File packDirectory = getPackDirectory("levelupmethods", packUsed);
        if(packDirectory != null && packDirectory.list() != null) {
            File newFile = getPackFile("levelupmethods", packDirectory);
            if(newFile != null) { file = newFile; parsingPack = true; }
        }

        ArrayList<String> idsToBeOmitted = getOmittedAssets(new File(packDirectory + File.separator + "omit.txt")); //Place where all ids that are located in the omit file are stored
        //Loads them
        for(int num=0; num<2; num++) {

            String jsonString = "{}";

            //Get all the assets from the mod and the default assets
            if(!parsingPack) {
                num++;
                Display.displayPackMessage("Loading deathmethods from the default pack");
                jsonString = getSingleJsonFileAsString(file.getPath().replace("\\", "/"));
            } else {
                Display.displayPackMessage("Loading from modpack");
                try (Scanner scan = new Scanner(file.getAbsolutePath())) {
                    jsonString = scan.next();

                } catch(NoSuchElementException e) {
                    jsonString = "{}";
                } catch (NullPointerException e) {
                    Display.displayError("Error while reading file '" + file.getAbsolutePath() + "' from modpack.");
                }
            }

            Display.changePackTabbing(true);
            JSONArray levelupmethodFile;
            try { levelupmethodFile = (JSONArray)(((JSONObject)parser.parse(jsonString)).get("levelupmethods")); } catch (ParseException e) { Display.displayPackError("Having trouble parsing from file"); e.printStackTrace(); continue; }
            if(levelupmethodFile == null) { continue; }
            for (Object o : levelupmethodFile) {
                JSONObject obj = (JSONObject) o;
                String id = (String) obj.get("id");
                if ((!parsingPack && idsToBeOmitted.contains(id)) || usedIds.contains(id)) {
                    continue;
                }
                if (id == null) {
                    Display.displayPackError("A levelupmethod to does not have an id. Omitting...");
                }
                Display.displayPackMessage("Loading levelupmethod '" + id + "'");
                Display.changePackTabbing(true);
                TFMethod method = (TFMethod) loadMethod(TFMethod.class, obj, IDMethod.class);
                if (method != null) {
                    levelupMethods.add(new IDMethod(method, id));
                } else {
                    Display.displayPackMessage("This levelupmethod does not have a valid method");
                }
                Display.changePackTabbing(false);
                usedIds.add(id);
            }
            Display.changePackTabbing(false);
            file=levelupmethodsFile;
            parsingPack=false;
        }
        parsingPack = false;
        Display.displayProgressMessage("Level up methods loaded.");
        Display.changePackTabbing(false);
        return true;
    }

    /**
     * Loads the choices of all the locations in the packs.
     * @return      Whether or not successful
     */
    public static boolean loadChoicesOfAllLocations() {
        parsingPack = false;
        Display.displayProgressMessage("Loading the choices of all locations");
        Display.changePackTabbing(true);
        //if(!choicesOfAllLocationsFile.exists()) { Display.displayError("Could not find the default choices of all locations file."); Display.changePackTabbing(false); return false; }
        ArrayList<String> usedNames = new ArrayList<>();
        File file = choicesOfAllLocationsFile;

        //Get the choices of all locations from the modpack
        File packDirectory = getPackDirectory("choicesOfAllLocations", packUsed);
        if(packDirectory != null && packDirectory.list() != null) {
            File newFile = getPackFile("choicesOfAllLocations", packDirectory);
            if(newFile != null) { file = newFile; parsingPack = true; }
        }

        ArrayList<String> namesToBeOmitted = getOmittedAssets(new File(packDirectory + File.separator + "omit.txt"));

        for(int num=0; num<2; num++) {

            String jsonString = "{}";

            //Get all the assets from the mod and the default assets
            if(!parsingPack) {
                num++;
                Display.displayPackMessage("Loading choices of all locations from the default pack");
                jsonString = getSingleJsonFileAsString(file.getPath().replace("\\", "/"));
            } else {
                Display.displayPackMessage("Loading from modpack");
                try (Scanner scan = new Scanner(file.getAbsolutePath())) {
                    jsonString = scan.next();
                } catch(NoSuchElementException e) {
                    jsonString = "{}";
                } catch (NullPointerException e) {
                    Display.displayError("Error while reading file '" + file.getAbsolutePath() + "' from modpack.");
                }
            }

            Display.changePackTabbing(true);
            JSONArray choicesFile;
            try { choicesFile = (JSONArray)(((JSONObject)parser.parse(jsonString)).get("choices")); } catch (ParseException e) { Display.displayPackError("Having trouble parsing from file"); e.printStackTrace(); continue; }
            if(choicesFile == null) { continue; }
            for (Object o : choicesFile) {
                JSONObject obj = (JSONObject) o;
                String name = (String) obj.get("name");
                Display.displayPackMessage("Loading choice '" + name + "'");
                if ((!parsingPack && namesToBeOmitted.contains(name)) || usedNames.contains(name)) {
                    continue;
                }
                Display.changePackTabbing(true);
                String desc = (String) obj.get("description");
                String usage = (String) obj.get("usage");
                if (obj.get("methods") == null) {
                    Display.displayPackError("This choice has no methods. Omitting...");
                    Display.changePackTabbing(false);
                    continue;
                }
                if (name == null) {
                    Display.displayPackError("Ths choice has no name. Omitting...");
                    Display.changePackTabbing(false);
                    continue;
                }
                Choice c = new Choice(name, desc, usage, loadMethods(ChoiceMethod.class, (JSONArray) obj.get("methods"), Choice.class), loadMethods(Requirement.class, (JSONArray) obj.get("requirements"), Choice.class), (String) obj.get("failMessage"));
                if (!(c.getMethods() != null && c.getMethods().size() > 0)) {
                    Display.changePackTabbing(false);
                    continue;
                }
                JSONArray excludes = (JSONArray) obj.get("excludes");
                Display.changePackTabbing(false);
                choicesOfAllLocations.add(new ChoiceOfAllLocations(excludes, c));
                usedNames.add(name);
                Display.changePackTabbing(false);
            }
            Display.changePackTabbing(false);
            file=choicesOfAllLocationsFile;
            parsingPack=false;
        }
        parsingPack = false;
        Display.displayProgressMessage("Choices of all locations loaded.");
        Display.changePackTabbing(false);
        return true;
    }

    /**
     * Loads a save from the given file name.
     * <p>Loads all values from the file and creates a new player instance with those values.
     * @param saveName  The name of the save file.
     * @return          True if successful. False is unsuccessful.
     */
    public static boolean loadGame(String saveName) {



        if(!PackMethods.areThereAnySaves()){ addToOutput("There are no saves, create one."); return false;}

        File f = new File(savesDir.getPath() + File.separator + saveName + ".json");
        if(!f.exists()) { addToOutput("Unable to find a save with name '" + saveName + "'."); return false; }

        currentSaveFile = f;

        try {
            JSONObject file = (JSONObject)parser.parse(new FileReader(f));
            gameName = (String)file.get("name");

            JSONObject stats = (JSONObject)file.get("stats");
            int level = 0;                                                              if(stats.get("level") != null)                      {level = Integer.parseInt((String)stats.get("level"));}
            int experience = 0;                                                         if(stats.get("experience") != null)                 {experience = Integer.parseInt((String)stats.get("experience"));}
            int score = 0;                                                              if(stats.get("score") != null)                      {score = Integer.parseInt((String)stats.get("score"));}
            int maxhp = Player.defaulthp;                                               if(stats.get("maxhealth") != null)                  {maxhp = Integer.parseInt((String)stats.get("maxhealth"));}
            int hp = Player.defaulthp;                                                  if(stats.get("health") != null)                     {hp = Integer.parseInt((String)stats.get("health"));}
            int deaths = 0;                                                             if(stats.get("deaths") != null)                     {deaths = Integer.parseInt((String)stats.get("deaths"));}
            int kills = 0;                                                              if(stats.get("kills") != null)                      {kills = Integer.parseInt((String)stats.get("kills"));}
            int coins = Player.defaultCoins;                                            if(stats.get("coins") != null)                      {coins = Integer.parseInt((String)stats.get("coins"));}
            int magic = Player.defaultMagic;                                            if(stats.get("magic") != null)                      {magic = Integer.parseInt((String)stats.get("magic"));}
            int metalscraps = Player.defaultMetalscraps;                                if(stats.get("metalscraps") != null)                {metalscraps = Integer.parseInt((String)stats.get("metalscraps"));}
            int healthPotions = Player.defaultHealthPotions;                            if(stats.get("healthPotions") != null)              {healthPotions = Integer.parseInt((String)stats.get("healthPotions"));}
            int strengthPotions = Player.defaultStrengthPotions;                        if(stats.get("strengthPotions") != null)            {strengthPotions = Integer.parseInt((String)stats.get("strengthPotions"));}
            int invincibilityPotions = Player.defaultInvincibilityPotions;              if(stats.get("invincibilityPotions") != null)       {invincibilityPotions = Integer.parseInt((String)stats.get("invincibilityPotions"));}
            int turnsWithStrengthLeft = Player.defaultTurnsWithStrengthLeft;            if(stats.get("turnsWithStrengthLeft") != null)      {turnsWithStrengthLeft = Integer.parseInt((String)stats.get("turnsWithInvincibilityLeft"));}
            int turnsWithInvincibilityLeft = Player.defaultTurnsWithInvincibilityLeft;  if(stats.get("turnsWithInvincibilityLeft") != null) {turnsWithInvincibilityLeft = Integer.parseInt((String)stats.get("turnsWithInvincibilityLeft"));}
            boolean gameBeaten = false;                                                 if(stats.get("gameBeaten") != null)                 {gameBeaten = Boolean.parseBoolean((String)stats.get("gameBeaten"));}

            JSONArray inventory = (JSONArray)file.get("inventory");

            ArrayList<Item> newInventory = new ArrayList<>();

            //Inventory items
            if(inventory != null && inventory.size()>0) {
                for (Object o : inventory) {
                    JSONObject jsonobj = (JSONObject) o;
                    if (jsonobj.get("name") == null) {
                        continue;
                    }
                    if (jsonobj.get("itemtype") != null) {
                        if (jsonobj.get("itemtype").equals("armor")) {
                            Armor item = getArmorByName((String) jsonobj.get("name"));
                            if (item != null) {
                                item = (Armor)item.clone(); //We want a COPY
                                newInventory.add(item);
                            }
                        } else if (jsonobj.get("itemtype").equals("weapon")) {
                            Weapon item = getWeaponByName((String) jsonobj.get("name"));
                            if (item != null) {
                                item = (Weapon)item.clone(); //We want a COPY
                                if (jsonobj.get("durability") != null) {
                                    item.setDurability(Integer.parseInt((String) jsonobj.get("durability")));
                                }
                                newInventory.add(item);
                            }
                        } else if (jsonobj.get("itemtype").equals("tool")) {
                            Tool item = getToolByName((String) jsonobj.get("name"));
                            if (item != null) {
                                item = (Tool)item.clone(); //We want a COPY
                                if (jsonobj.get("durability") != null) {
                                    item.setDurability(Integer.parseInt((String) jsonobj.get("durability")));
                                }
                                newInventory.add(item);
                            }
                        } else if (jsonobj.get("itemtype").equals("specialitem")) {
                            SpecialItem item = getSpecialItemByName((String) jsonobj.get("name"));
                            if (item != null) {
                                item = (SpecialItem) item.clone(); //We want a COPY
                                newInventory.add(item);
                            }
                        }
                    }
                }
            }

            Weapon currentWeapon = null;
            String currentWeaponString = Player.defaultCurrentWeaponName;               if(stats.get("currentWeapon") != null)              {currentWeaponString = (String)stats.get("currentWeapon");}
            for(Item i : newInventory) {
                if(i.getName().equals(currentWeaponString)) {
                    currentWeapon = (Weapon)i;
                }
            }
            if(currentWeapon == null) {
                for(Weapon w : weapons) {
                    if(w.getName().equals(Player.defaultCurrentWeaponName)) {
                        currentWeapon = w;
                    }
                }
            }

            ArrayList<Achievement> playerAchievements = new ArrayList<>();
            JSONArray achievementsArray = (JSONArray)file.get("achievements");

            // Adds the previously earned achievements to a new array
            if(achievementsArray != null  && achievementsArray.size()>0) {
                for (Object o : achievementsArray) {
                    for (Achievement a : achievements) {
                        if (o.equals(a.getName())) {
                            playerAchievements.add(a);
                        }
                    }
                }
            }

            ArrayList<CustomVariable> newPlayerCustomVariables = new ArrayList<>();
            JSONArray customVariables = (JSONArray)file.get("customvariables");

            ArrayList<CustomVariable> unusedCustomVariables = new ArrayList<>(playerCustomVariables);

            //loads the custom variables that were saved
            if((customVariables != null && customVariables.size()>0)) {
                for (Object customVariable : customVariables) {
                    JSONObject obj = (JSONObject) customVariable;
                    if (obj.get("name") != null && obj.get("type") != null && obj.get("value") != null) {
                        int type = Integer.parseInt((String) obj.get("type"));
                        Object value = obj.get("value");
                        //Null values are specified by "%null%"
                        if (value.equals("%null%")) {
                            value = null;
                        } else if (type == 0) {
                            value = obj.get("value");
                        } else if (type == 1) {
                            value = Integer.parseInt((String) value);
                        } else if (type == 2) {
                            value = Double.parseDouble((String) value);
                        } else if (type == 3) {
                            value = Boolean.parseBoolean((String) value);
                        } else if (type == 4) {
                            try {
                                value = Class.forName((String) value);
                            } catch (ClassNotFoundException e) {
                                continue;
                            }
                        }

                        for (CustomVariable cv : unusedCustomVariables) {
                            if (cv.getName().equals(obj.get("name"))) {
                                cv.setValue(value);
                                newPlayerCustomVariables.add(cv);
                                unusedCustomVariables.remove(cv); //This one is used
                                break;
                            }
                        }
                    }
                }
                //Add any remaining custom variables not saved
                newPlayerCustomVariables.addAll(unusedCustomVariables);

                //Reset the player custom variables
                playerCustomVariables = newPlayerCustomVariables;
            }

            //Create a new player instance with the loaded values
            player = new Player(deaths, kills, player.getLocation(), hp, maxhp, coins, magic, metalscraps, level, experience, score, healthPotions, strengthPotions, invincibilityPotions, turnsWithStrengthLeft, turnsWithInvincibilityLeft, currentWeapon, gameBeaten, newInventory, playerAchievements, playerCustomVariables, deathMethods, levelupMethods);
            addToOutput("Loaded save '" + saveName + "'");

        } catch (IOException | ParseException e) { addToOutput("Unable to read the save"); e.printStackTrace(); return false; }
        catch (Exception e) { addToOutput("The save is corrupted!"); e.printStackTrace(); return false; }

        return true;

    }

    /**
     * Creates a new save file.
     * <p>Fails if there is already a save with the name given.</p>
     * @param name  The name of the new save.
     */
    public static void newGame(String name) {

        // Tells the player if they would overwrite a save (And doesnt allow them to do so)
        if(PackMethods.getSaveFiles().contains(name)) {
            addToOutput("There is already a save with that name. Pick another.");
            return;
        }

        // attempts to create the save file
        File newGameFile = new File(savesDir.getPath() + File.separator + name + ".json");
        try { if(!newGameFile.createNewFile()) { addToOutput("Unable to create new file"); } } catch (IOException e) {
            addToOutput("Failed to create new file");
            e.printStackTrace();
            return;
        }

        currentSaveFile = newGameFile;
        gameName = name;

        //Initializes the game with a default player
        player = new Player(
                0, //deaths
                0, //kills
                player.getLocation(),
                Player.defaulthp,
                Player.defaultMaxhp,
                Player.defaultCoins,
                Player.defaultMagic,
                Player.defaultMetalscraps,
                Player.defaultLevel,
                Player.defaultExperience,
                Player.defaultScore,
                Player.defaultHealthPotions,
                Player.defaultStrengthPotions,
                Player.defaultInvincibilityPotions,
                Player.defaultTurnsWithStrengthLeft,
                Player.defaultTurnsWithInvincibilityLeft,
                getWeaponByName(Player.defaultCurrentWeaponName),
                false,
                new ArrayList<>(),
                achievements,
                playerCustomVariables,
                deathMethods,
                levelupMethods
        );

        //Writes it
        saveGame();
    }

    /**
     * Saves the game.
     * <p>Rewrites the whole file.</p>
     * @return      True if successful, False if unsuccessful.
     */
    public static boolean saveGame() {

        if(currentSaveFile == null) { addToOutput("No save loaded"); return false; }

        if(!currentSaveFile.exists()) {
            try {
                if(!currentSaveFile.createNewFile()) { addToOutput("Unable to save game!"); return false; }
            } catch (IOException e) {
                addToOutput("Unable to save game!"); return false;
            }
        }

        //Rewrites the whole save file with the new stuff

        JSONObject base = new JSONObject();

        JSONObject stats = new JSONObject();
        stats.put("level", Integer.toString(player.getLevel()));
        stats.put("experience", Integer.toString(player.getExperience()));
        stats.put("score", Integer.toString(player.getScore()));
        stats.put("health", Integer.toString(player.getHp()));
        stats.put("maxhealth", Integer.toString(player.getMaxHp()));
        stats.put("deaths", Integer.toString(player.getDeaths()));
        stats.put("kills", Integer.toString(player.getKills()));
        stats.put("coins", Integer.toString(player.getCoins()));
        stats.put("magic", Integer.toString(player.getMagic()));
        stats.put("metalscraps", Integer.toString(player.getMetalScraps()));
        stats.put("gameBeaten", Boolean.toString(player.getGameBeaten()));
        stats.put("healthPotions", Integer.toString(player.getHealthPotions()));
        stats.put("strengthpotions", Integer.toString(player.getStrengthPotions()));
        stats.put("invincibilityPotions", Integer.toString(player.getInvincibilityPotions()));
        stats.put("turnsWithStrengthLeft", Integer.toString(player.getTurnsWithStrengthLeft()));
        stats.put("turnsWithInvincibilityLeft", Integer.toString(player.getTurnsWithInvincibilityLeft()));
        if(player.getCurrentWeapon() != null) {stats.put("currentWeapon", player.getCurrentWeapon().getName());} else { stats.put("currentWeapon", "fists"); }

        JSONArray inventory = new JSONArray();
        //For items in inventory
        if(player.getInventory() != null) {
            for(Item i : player.getInventory()) {
                JSONObject jsonobj = new JSONObject();
                jsonobj.put("name", i.getName());
                jsonobj.put("itemtype", i.getItemType());
                if(i instanceof Weapon) { jsonobj.put("durability",Integer.toString(((Weapon)i).getDurability())); }
                if(i instanceof Tool) { jsonobj.put("durability",Integer.toString(((Tool)i).getDurability())); }
                inventory.add(jsonobj);
            }
        }

        JSONArray earnedAchievements = new JSONArray();
        //For all achievements
        if(player.getAchievements() != null) {
            for(Achievement a : player.getAchievements()) {
                earnedAchievements.add(a.getName());
            }
        }

        JSONArray customVariables = new JSONArray();
        //for all customvariables that are to be saved
        if(player.getCustomVariables() != null) {
            for(CustomVariable cv : player.getCustomVariables()) {
                if(cv.getIsSaved()) {
                    JSONObject obj = new JSONObject();
                    obj.put("name",cv.getName());
                    //Saves the type with the correct number
                    if(cv.getValue() == null) { obj.put("value", "%null%"); }
                    else { obj.put("value", cv.getValue().toString()); }
                    if(cv.getValueType().equals(String.class)) {
                        obj.put("type", "0");
                    } else if(cv.getValueType().equals(int.class)) {
                        obj.put("type", "1");
                    } else if(cv.getValueType().equals(double.class)) {
                        obj.put("type", "2");
                    } else if(cv.getValueType().equals(boolean.class)) {
                        obj.put("type", "3");
                    } else if(cv.getValueType().equals(Class.class)) {
                        obj.put("type", "4");
                    } else {
                        continue;
                    }
                    customVariables.add(obj);
                }
            }
        }

        base.put("customvariables", customVariables);
        base.put("achievements", earnedAchievements);
        base.put("inventory", inventory);
        base.put("stats", stats);
        base.put("name", gameName);

        //Writes it
        try (FileWriter w = new FileWriter(currentSaveFile, false)) {
            w.write(base.toJSONString());
            addToOutput("Game saved in file '" + currentSaveFile.getName() + "'!");
        } catch (IOException e) { e.printStackTrace(); return false; }

        return true;

    }

    /**
     * Adds a save to the {@link #saves} arraylist.
     * @param name  The name to be added.
     */
    public static void addSave(String name) { saves.add(name); }
    /**
     * Removes a save file.
     * <p>Removes the save from the {@link #saves} arraylist.<p>
     * @param name  The name of the save.
     */
    public static void removeSave(String name) {
        if(name == null) { return; }
        if(name.lastIndexOf(".") != -1) { name = name.substring(0,name.lastIndexOf(".")); }
        PackMethods.getSaveFiles();
        //Makes sure the player isnt attempting to delete the save that is currently being used and the player hasnt beaten the game
        //When the game has been beaten, saves are not deleted when the player dies
        if(currentSaveFile != null && ( ( (name+".json").equals(currentSaveFile.getName())))) { currentSaveFile = null; } //No longer loaded a save
        boolean saveExists = false;
        //Find the save in the saves array
        for(int i=0;i<saves.size();i++){
            if(saves.get(i).equals(name)) {
                saveExists = true;
                File gameFile = new File(savesDir.getAbsolutePath() + File.separator + saves.get(i) + ".json");
                if(!gameFile.delete()) {
                    addToOutput("File found, but was unable to delete it");
                    return;
                }
                saves.remove(i);
                addToOutput("Removed save '" + name + "'");
                break;
            }
        }
        if(!saveExists) {
            addToOutput("File with that name not found. No action performed");
        }
    }

    /**
     * Sorts the enemies by difficulty from lowest to highest and sets {@link #enemies}.
     */
    public static void sortEnemies() {
        if(enemies.size() < 2) { return; } //There is no need to sort if there are no enemies or just one
        //Iterate through the enemies and find out which has the lowest difficulty in the array
        //Then add that enemy to the sorted array and remove it from the remaining array
        //The loop continues iterating only on the unsorted (remaining array) enemies to save assets
        ArrayList<Enemy> sorted = new ArrayList<>();
        ArrayList<Enemy> remaining = new ArrayList<>(enemies);
        while(remaining.size() != 0) {
            int lowestDiffIndex = 0;
            int lowestKnownDifficulty = 0;
            for(int i=0; i<remaining.size(); i++) {
                if(i == 0) {
                    lowestKnownDifficulty = remaining.get(i).getDifficulty();
                } else if(remaining.get(i).getDifficulty() < lowestKnownDifficulty) {
                    lowestDiffIndex = i;
                }
            }
            sorted.add(remaining.get(lowestDiffIndex));
            remaining.remove(lowestDiffIndex);
        }
        //Set the enemies to the new sorted array
        enemies = sorted;
    }

    /*** Sets the possible enemies arraylist in TextFighter with enemies that meet their requirements for the player to fight.*/
    public static void setPossibleEnemies() {
        ArrayList<Enemy> possible = new ArrayList<>();
        for(Enemy e : enemies) {
            boolean valid = true;
            if(e.getLevelRequirement() <= player.getLevel()) {
                if(e.getRequirements() != null) {
                    //Make sure the requirements are met for the player to be able to fight the enemy
                    for(Requirement r : e.getRequirements()) {
                        if(!r.invokeMethod()) {
                            valid=false;
                            break;
                        }
                    }
                }
                if(valid) {
                    possible.add(e);
                }
            }
        }
        possibleEnemies = possible;
    }

    /**
     * Gets and handles player input.
     * @return      True if a valid choice. False if not.
     */
    public synchronized static boolean invokePlayerInput() {

        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))){
            String input = "";
            if(Display.gui != null) {
                if(inputHistory.size() == 0) { inputHistory.addCommand(""); } //Create a new command in the history.
                inputHistory.setCurrentIndex(0);
                synchronized (waiter) {
                    try {
                        Display.gui.canEnterInput = true;
                        waiter.wait();
                        input = Display.gui.inputArea.getText();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Display.gui.canEnterInput = false;
            } else {
                //Display the prompt to the screen
                Display.promptUser();
                //Get the player's input
                input = in.readLine();
                Display.resetColors();
            }
            Display.previousCommandString = input;

            if(Display.guiMode && !input.isEmpty()) {
                inputHistory.set(0,input);
                inputHistory.addCommand(""); //Create a new command in the history. The old one was already added so we vibin'.
                Display.gui.copiedInputHistory = (HistoryLinkedList)inputHistory.clone();
                Display.gui.copiedInputHistory.setCurrentIndex(0);
            }
            Display.writeToLogFile("[Input] " + input);
            boolean validChoice = false;
            //Iterate through all current possible choices
            for(Choice c : player.getLocation().getPossibleChoices()){
                //Makes sure the input has any arguments
                if(input.contains(" ")) {
                    if (c.getName().equals(input.substring(0,input.indexOf(" ")))) {
                        validChoice = true;
                        break;
                    }
                //If no arguments
                } else {
                    if (c.getName().equals(input)) {
                        validChoice = true;
                        break;
                    }
                }
            }
            if(validChoice) { //The choice does exist
                ArrayList<String> inputArrayList = new ArrayList<>(Arrays.asList(input.split(" ")));
                String commandName = inputArrayList.get(0);
                inputArrayList.remove(0);
                for(Choice c : player.getLocation().getPossibleChoices()) {
                    if(c.getName().equals(commandName)) {
                        //Pass the input to the choice so that the choice can put the input where placeholders ("%ph%") are in the arguments
                        return(c.invokeMethods(inputArrayList));
                    }
                }
                return true;
            } else { //The choice doesn't exist
                if(input.contains(" ")) {
                    addToOutput("Invalid choice - '" + input.substring(0, input.indexOf(" ")) + "'");
                } else {
                    addToOutput("Invalid choice - '" + input + "'");
                }
                return false;
            }
        } catch (IOException e) {
            Display.displayError("An error occured while reading input!");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sets up a fight.
     * @param en    The name of the enemy to be fought.
     * @return      False if invalid enemy, else true.
     */
    public static boolean setUpFight(String en) {
        if(en == null) { addToOutput("No enemy name given to fight."); }
        boolean validEnemy = false;
        //Gets the enemy given
        for(Enemy enemy : possibleEnemies) {
            if(enemy.getName().equals(en)) {
                try {
                    Enemy newEnemy = (Enemy)enemy.clone();
                    newEnemy.setOriginalHp(newEnemy.getHp()); //Make sure the original health stays as it is supposed to so that the difficulty of the enemy doesnt go wack.
                    currentEnemy = newEnemy;
                    newEnemy.invokePremethods();
                    validEnemy = true;
                } catch (CloneNotSupportedException e) {
                    addToOutput("Could not create an enemy of that type! ('" + e + "')");
                    e.printStackTrace();
                    return false;
                }
            }
        }
        if(validEnemy) {
            player.setInFight(true);
            PackMethods.movePlayer("fight");
            return true;
        } else {
            addToOutput("Invalid enemy: '" + en + "'");
        }
        return false;
    }

    /**
     * Moves the player to the "win" location.
     */
    public static void playerWins() {
        PackMethods.movePlayer("win");
    }

    //Exit game. The reason I need this method is because it wont let me use System.exit(code) in packs (Because Class.forName() claims the class doesn't exist)
    /**
     * Calls System.exit()
     * @param code  The exit code.
     */
    public static void exitGame(int code) { Display.writeToLogFile("Game exitted by mod."); System.exit(code); }

    /**
     * Determine if a save is loaded.
     * @return      True if a game is loaded. False if not.
     */
    public static boolean gameLoaded() { return (currentSaveFile != null); }

    /**
     * Orchestrates player turns.
     */
    public static void playGame() {
        Display.writeToLogFile("[<----------------------------------START OF TURN---------------------------------->]");
        Display.clearScreen();
        if(!Display.guiMode) { Display.displayOutputMessage("<----------START OF TURN---------->"); }
        Display.displayPreviousCommand();
        //Determine if any achievements should be recieved
        if(gameLoaded()) {
            for(Achievement a : achievements) {
                if(!player.isAchievementEarned(a.getName())) {
                    boolean earned = true;
                    //Make sure the requirements have been met
                    for(Requirement r : a.getRequirements()) {
                        if(!r.invokeMethod() == r.getNeededBoolean()) {
                            earned = false;
                            break;
                        }
                    } //Give the player the achievement if all requirements are met
                    if(earned) {
                        player.achievementEarned(a);
                    }
                }
            }
        }
        //Print the output of the previous player choice
        if(output != null) {
            Display.displayOutputMessage(output);
        }
        output="";
        //Display the interface and get input
        Display.displayInterfaces(player.getLocation());
        boolean validInput = invokePlayerInput();
        if(Display.guiMode) { Display.gui.inputArea.setText(""); } // Empty the input area. If they want to fix it, then they just have to hit the up arrow
		//If the player inputted something valid and the player is in a fight, then do enemy action stuff
        if(actedSinceStartOfFight && validInput && player.getInFight()) {
            if(currentEnemy != null) {
                if(currentEnemy.getHp() <= 0) {
                    player.setInFight(false);
                    addToOutput("Your enemy has died!");
                    player.increaseKills(1);
					currentEnemy.invokeRewardMethods();
                    currentEnemy.invokePostmethods();
					PackMethods.movePlayer("menu");
					currentEnemy = null;
                } else {
                    currentEnemy.performRandomAction(true);
                    currentEnemy.decreaseTurnsWithInvincibilityLeft(1);
                }
            }
            player.decreaseTurnsWithStrengthLeft(1);
            player.decreaseTurnsWithInvincibilityLeft(1);
        }
		if(player.getHp() < 1) {
            player.setInFight(false);
            player.died();
            currentEnemy = null;
        }
        if(player.getInFight() && validInput) { actedSinceStartOfFight = true; }
        if(!Display.guiMode) { Display.displayOutputMessage("<----------END OF TURN---------->"); }
        Display.writeToLogFile("[<-----------------------------------END OF TURN----------------------------------->]");
    }

    /**
     * Detemine if the game is in test mode, load assets, and main game loop.
     * If the game is not already installed, then it will be installed.
     * @param args  Command line input.
     */
    public static void main(String[] args) {

        if(TextFighter.class.getResource("TextFighter.class").toString().startsWith("jar:")) { //then the game is run from a jar
            runFromJar = true;
        }

        version = readVersionFromFile(); //Important later

        //Command Line arguments
        for(String a : args) {
            if(a.equals("test")) {
                testMode = true;
            } else if(a.equals("nogui")) {
                Display.guiMode = false;
            } else if(a.equals("compileguide") && !testMode) {
                guideCompileMode = true;
                Display.guiMode = false;
            } else if(a.equals("log")) {
                Display.logMode = true; //Automatically log everything even if there are no errors
            }
        }

        if(Display.guiMode) {
            Display.createGui();
            Display.gui.updateTitle();
	        if(testMode) { Display.gui.inputArea.setEnabled(false); }
        }

        if(!testMode) { Display.clearScreen(); }

        //Install the game if not already installed
        if(!installGame()) {
            Display.displayError("An error occurred during installation.");
            System.exit(-1);
        }

        loadDirectories();

        if(guideCompileMode) {
            loadConfig();
            Display.displayProgressMessage("Compiling the guide for mod '" + packUsed.getName() + "'");
            compileGuide();
            return; //Quit immediately
        }

        //Load all the assets and make sure they are loaded correctly
        if (!loadassets()) {
            Display.displayError("An error occured while trying to load the assets!\nMake sure they are in the correct directory.");
            System.exit(1);
        }

        //Update the title to include the mod (If not using a mod, then nothing changes)
        if(Display.guiMode) {
            Display.gui.updateTitle();
            Display.gui.createGuideArea(); //Create the guide area using the loaded mod
        }

        Display.displayProgressMessage(Display.errorsOnLoading + " errors occurred while loading the assets.");
        addToOutput(Display.errorsOnLoading + " errors occurred while loading the assets.");
        player = new Player(getLocationByName("start"), playerCustomVariables, deathMethods, levelupMethods);
        //If the game is in test mode, then the game should not run for the user (Only get modpack feedback)
        if(!testMode) {
            PackMethods.getSaveFiles();
            player.setLocation("start");

            addToOutput("\nVisit https://github.com/seanmjohns/Text-Fighter/wiki\n" +
                                "for a guide to configuration, playing the game, and\n" +
                                "creating a mod of your own!\n" +
                                "A guide for each mod should be located in the mod's\n" +
                                "folder, which are to be placed in the `packs` folder.\n" +
                                "The configuration files are located in one of the\n" +
                                "following locations (Depending on your OS):\n" +
                                "   - Windows: `C:\\Users\\Username\\Appdata\\Roaming\\textfighter\\config\\`\n" +
                                "   - MacOS: `~/Library/Application Support/textfighter/config/`\n" +
                                "   - Linux: `~/.textfighter/config/`\n" +
                                "The vanilla textfighter guide is displayed in the `guide` tab\n");

            while(player.getAlive() || player.getGameBeaten()) {
                playGame();
            }
        } else if(Display.guiMode) {
            //We want the modmakers to be able to see what is happening, so dont close the window
            try {
                synchronized (waiter) {
                    waiter.wait();
                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //Do nothing at all lmao
    /**
     * Does literally nothing.
     */
    public static void doNothing() {}
}
