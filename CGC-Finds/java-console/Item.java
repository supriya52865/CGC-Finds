import java.time.LocalDate;

/**
 * One report: either a LOST item or a FOUND item.
 */
public class Item {

    public enum Type { LOST, FOUND }

    private final int id;
    private final Type type;
    private final String name;
    private final Category category;
    private final String description;
    private final String location;
    private final LocalDate date;
    private final String contact;
    private boolean resolved;

    public Item(int id, Type type, String name, Category category, String description,
                String location, LocalDate date, String contact, boolean resolved) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.category = category;
        this.description = description;
        this.location = location;
        this.date = date;
        this.contact = contact;
        this.resolved = resolved;
    }

    public int getId() { return id; }
    public Type getType() { return type; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public LocalDate getDate() { return date; }
    public String getContact() { return contact; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    // ---------- saving / loading (one line per item, fields separated by '|') ----------

    private static String clean(String s) {
        return s.replace("|", "/").replace("\n", " ").trim();
    }

    public String toRecord() {
        return String.join("|",
                String.valueOf(id), type.name(), clean(name), category.name(),
                clean(description), clean(location), date.toString(), clean(contact),
                String.valueOf(resolved));
    }

    public static Item fromRecord(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 9) {
            throw new IllegalArgumentException("Bad record: " + line);
        }
        return new Item(Integer.parseInt(p[0]), Type.valueOf(p[1]), p[2],
                Category.valueOf(p[3]), p[4], p[5], LocalDate.parse(p[6]), p[7],
                Boolean.parseBoolean(p[8]));
    }

    /** Multi-line card used when showing full details. */
    public String toCard() {
        return String.format(
                "  [#%d] %s - %s%s%n"
              + "        Category   : %s%n"
              + "        Description: %s%n"
              + "        Place      : %s%n"
              + "        Date       : %s%n"
              + "        Contact    : %s",
                id, type, name, resolved ? "  (RESOLVED)" : "",
                category.getLabel(), description, location, date, contact);
    }

    /** One-line summary used in tables. */
    public String toRow() {
        return String.format("  #%-3d %-5s %-22s %-24s %-18s %s%s",
                id, type, shorten(name, 22), category.getLabel(), shorten(location, 18),
                date, resolved ? "  [resolved]" : "");
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }
}
