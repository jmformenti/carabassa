package org.atypical.carabassa.cli.util;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/** Utility helpers for interactive CLI prompts. */
public class InteractiveCommand {

  /**
   * Prompts the user for a yes/no confirmation.
   *
   * @param text prompt to display
   * @return true when the user answers yes
   */
  public static boolean doConfirm(String text) {
    System.out.print(text);
    try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
      String userInput = scanner.next();
      return userInput.equalsIgnoreCase("y") || userInput.equalsIgnoreCase("yes");
    }
  }
}
