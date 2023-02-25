package org.atypical.carabassa.cli.util;

import java.util.Scanner;

public class InteractiveCommand {

    public static boolean doConfirm(String text) {
        System.out.print(text);
        try (Scanner scanner = new Scanner(System.in)) {
            String userInput = scanner.next();
            return userInput.equalsIgnoreCase("y") || userInput.equalsIgnoreCase("yes");
        }
    }
}
