import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * FindIt - Lost & Found
 * A console program to report lost/found items, search by category,
 * and get automatic keyword-based match suggestions.
 */
public class Main {

    private static final Scanner in = new Scanner(System.in);
    private static final ItemStore store = new ItemStore("data/items.txt");

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("        FindIt  -  Lost & Found");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ", 0, 8);
            System.out.println();
            switch (choice) {
                case 1 -> reportItem(Item.Type.LOST);
                case 2 -> reportItem(Item.Type.FOUND);
                case 3 -> searchByCategory();
                case 4 -> viewAll();
                case 5 -> showSuggestionsFor();
                case 6 -> markResolved();
                case 7 -> searchByKeyword();
                case 8 -> loadSampleData();
                case 0 -> {
                    running = false;
                    System.out.println("Goodbye! All reports are saved.");
                }
                default -> { }
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("---------------- MENU ----------------");
        System.out.println(" 1. Report a LOST item");
        System.out.println(" 2. Report a FOUND item");
        System.out.println(" 3. Search by item category");
        System.out.println(" 4. View all reports");
        System.out.println(" 5. Show suggested matches for a report");
        System.out.println(" 6. Mark a report as resolved");
        System.out.println(" 7. Search by keyword");
        System.out.println(" 8. Load sample data (for demo)");
        System.out.println(" 0. Exit");
        System.out.println("--------------------------------------");
    }

    // ---------------------------------------------------------------- 1 & 2: report

    private static void reportItem(Item.Type type) {
        System.out.println("=== Report a " + type + " item ===");
        String name = readText("Item name (e.g. Black wallet): ");
        Category category = chooseCategory();
        String description = readText("Description (colour, brand, marks, contents): ");
        String where = readText(type == Item.Type.LOST ? "Where did you lose it? " : "Where did you find it? ");
        LocalDate date = readDate("Date (YYYY-MM-DD, press Enter for today): ");
        String contact = readText("Your contact (phone/email): ");

        Item item = new Item(store.nextId(), type, name, category, description, where, date, contact, false);
        store.add(item);
        System.out.println();
        System.out.println("Report saved with ID #" + item.getId() + ".");

        // The unique feature: automatically suggest matches right after reporting
        printSuggestions(item);
    }

    // ---------------------------------------------------------------- 3: category search

    private static void searchByCategory() {
        System.out.println("=== Search by category ===");
        Category category = chooseCategory();
        List<Item> results = store.byCategory(category);
        System.out.println();
        if (results.isEmpty()) {
            System.out.println("No reports in category: " + category.getLabel());
            return;
        }
        System.out.println(results.size() + " report(s) in " + category.getLabel() + ":");
        printTable(results);
    }

    // ---------------------------------------------------------------- 4: view all

    private static void viewAll() {
        List<Item> all = store.all();
        if (all.isEmpty()) {
            System.out.println("No reports yet.");
            return;
        }
        System.out.println("All reports (" + all.size() + "):");
        printTable(all);
        int id = readInt("\nEnter an ID to see full details (0 to go back): ", 0, Integer.MAX_VALUE);
        if (id != 0) {
            Item it = store.findById(id);
            System.out.println(it == null ? "No report with that ID." : it.toCard());
        }
    }

    // ---------------------------------------------------------------- 5: suggestions

    private static void showSuggestionsFor() {
        if (store.all().isEmpty()) {
            System.out.println("No reports yet.");
            return;
        }
        int id = readInt("Enter report ID: ", 1, Integer.MAX_VALUE);
        Item item = store.findById(id);
        if (item == null) {
            System.out.println("No report with that ID.");
            return;
        }
        System.out.println(item.toCard());
        printSuggestions(item);
    }

    private static void printSuggestions(Item item) {
        List<MatchEngine.Match> matches = MatchEngine.suggest(item, store.all());
        String wanted = item.getType() == Item.Type.LOST ? "FOUND" : "LOST";
        System.out.println();
        if (matches.isEmpty()) {
            System.out.println("No matching " + wanted + " reports yet. Check again later!");
            return;
        }
        System.out.println(">>> Possible matching " + wanted + " reports:");
        for (MatchEngine.Match m : matches) {
            System.out.println();
            System.out.println("  Match score: " + m.score + "%   (common keywords: "
                    + String.join(", ", m.sharedKeywords) + ")");
            System.out.println(m.item.toCard());
        }
    }

    // ---------------------------------------------------------------- 6: resolve

    private static void markResolved() {
        int id = readInt("Enter report ID to mark as resolved: ", 1, Integer.MAX_VALUE);
        Item item = store.findById(id);
        if (item == null) {
            System.out.println("No report with that ID.");
            return;
        }
        item.setResolved(true);
        store.save();
        System.out.println("Report #" + id + " marked as resolved. It will no longer appear in suggestions.");
    }

    // ---------------------------------------------------------------- 7: keyword search

    private static void searchByKeyword() {
        String text = readText("Enter keyword(s): ");
        var words = MatchEngine.keywords(text);
        if (words.isEmpty()) {
            System.out.println("Please enter a more specific keyword.");
            return;
        }
        List<Item> results = new java.util.ArrayList<>();
        for (Item it : store.all()) {
            var itemWords = MatchEngine.keywords(it.getName() + " " + it.getDescription());
            if (itemWords.stream().anyMatch(words::contains)) {
                results.add(it);
            }
        }
        if (results.isEmpty()) {
            System.out.println("Nothing found for: " + text);
        } else {
            System.out.println(results.size() + " report(s) found:");
            printTable(results);
        }
    }

    // ---------------------------------------------------------------- 8: sample data

    private static void loadSampleData() {
        if (!store.all().isEmpty()) {
            System.out.println("Sample data is only loaded when there are no reports yet.");
            return;
        }
        LocalDate d = LocalDate.now();
        add(Item.Type.LOST, "iPhone 13", Category.ELECTRONICS, "Blue mobile phone with cracked back cover", "Library", d.minusDays(2), "98765-43210");
        add(Item.Type.FOUND, "Smartphone", Category.ELECTRONICS, "Blue phone, cracked cover, found on a table", "Library reading hall", d.minusDays(1), "security@campus.edu");
        add(Item.Type.LOST, "Black Wallet", Category.WALLETS_BAGS, "Leather wallet with college ID card and some cash", "Canteen", d.minusDays(3), "rahul@mail.com");
        add(Item.Type.FOUND, "Purse", Category.WALLETS_BAGS, "Black leather purse containing ID card", "Canteen", d.minusDays(1), "canteen counter");
        add(Item.Type.LOST, "House Keys", Category.KEYS, "Bunch of 3 keys with a red keychain", "Parking lot", d.minusDays(4), "99887-76655");
        add(Item.Type.FOUND, "Water Bottle", Category.OTHER, "Steel bottle, silver colour", "Ground", d.minusDays(1), "sports office");
        System.out.println("Sample data loaded (6 reports). Try option 5 on report #1 or #3.");
    }

    private static void add(Item.Type type, String name, Category cat, String desc,
                            String where, LocalDate date, String contact) {
        store.add(new Item(store.nextId(), type, name, cat, desc, where, date, contact, false));
    }

    // ---------------------------------------------------------------- helpers

    private static void printTable(List<Item> items) {
        System.out.println(String.format("  %-4s %-5s %-22s %-24s %-18s %s",
                "ID", "TYPE", "NAME", "CATEGORY", "PLACE", "DATE"));
        System.out.println("  " + "-".repeat(90));
        for (Item it : items) {
            System.out.println(it.toRow());
        }
    }

    private static Category chooseCategory() {
        Category[] cats = Category.values();
        System.out.println("Categories:");
        for (int i = 0; i < cats.length; i++) {
            System.out.println("  " + (i + 1) + ". " + cats[i].getLabel());
        }
        int n = readInt("Choose category (1-" + cats.length + "): ", 1, cats.length);
        return cats[n - 1];
    }

    private static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = in.hasNextLine() ? in.nextLine().trim() : "";
            if (!s.isEmpty()) {
                return s;
            }
            if (!in.hasNextLine()) {
                return "-";
            }
            System.out.println("  This field cannot be empty.");
        }
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (!in.hasNextLine()) {
                return 0; // input ended: behave like "exit"/"back"
            }
            String s = in.nextLine().trim();
            try {
                int n = Integer.parseInt(s);
                if (n >= min && n <= max) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
                // fall through to message
            }
            System.out.println("  Please enter a number between " + min + " and " + (max == Integer.MAX_VALUE ? "..." : String.valueOf(max)) + ".");
        }
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = in.hasNextLine() ? in.nextLine().trim() : "";
            if (s.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("  Invalid date. Use format YYYY-MM-DD, e.g. 2026-11-12.");
            }
        }
    }
}
