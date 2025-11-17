package ui;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UILogger {

    private final JLabel target;

    public UILogger(JLabel lbl) {
        this.target = lbl;
    }

    public void log(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() ->
                target.setText("[" + time + "] " + msg)
        );
    }
}
