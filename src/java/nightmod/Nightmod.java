package nightmod;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.Properties;
import javax.swing.*;

/**
 * This class handles loading of Nightmod namespaces while the splash image
 * is displayed on the screen. See entry point main() method in this class. How
 * it works:
 * 0. Assumes the splash screen is loaded (see project.clj - :manifest entry)
 * 1. Loads nightmod.core/-main using reflection (it takes time)
 * 2. Closes splash screen
 * 3. Invokes nightmod.core/-main
 */
public class Nightmod {

    private Method loadNightmodMain() throws Exception {
        final String cn = "nightmod.core";
        Class<?> clazz = Class.forName(cn);
        if (clazz == null) {
            throw new RuntimeException("Cannot load class " + cn);
        }
        Method m = clazz.getMethod("main", String[].class);
        if (m == null) {
            throw new RuntimeException("Cannot obtain method 'main'");
        }
        return m;
    }

    private void invokeNightmodMain(Method m, String[] args) throws Exception {
        m.invoke(null, (Object) args);
    }

    private void init(String[] args) throws Exception {
        final SplashScreen splash = SplashScreen.getSplashScreen();

        Method m = loadNightmodMain();

        if (splash != null) {
            splash.close();
        }

        invokeNightmodMain(m, args);
    }

    public static void main(String[] args) throws Exception {
        Properties props = System.getProperties();
        // properties to make font rendering smoother on Linux+OracleJVM/OpenJDK
        // should make no difference on other OSes and JVMs
        // set properties only if the user has not overidden them
        if (props.getProperty("awt.useSystemAAFontSettings") == null
                && props.getProperty("sun.java2d.xrender") == null) {
            props.setProperty("awt.useSystemAAFontSettings", "on");
            props.setProperty("sun.java2d.xrender", "true");
        }
        // initialize and launch Nightmod
        new Nightmod().init(args);
    }

}
