import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps all reports in memory and saves them to data/items.txt
 * so nothing is lost when the program is closed.
 */
public class ItemStore {

    private final Path file;
    private final List<Item> items = new ArrayList<>();

    public ItemStore(String filePath) {
        this.file = Paths.get(filePath);
        load();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    items.add(Item.fromRecord(line));
                }
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("Warning: could not read saved data (" + e.getMessage() + ").");
        }
    }

    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            List<String> lines = new ArrayList<>();
            for (Item it : items) {
                lines.add(it.toRecord());
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Warning: could not save data (" + e.getMessage() + ").");
        }
    }

    public int nextId() {
        int max = 0;
        for (Item it : items) {
            max = Math.max(max, it.getId());
        }
        return max + 1;
    }

    public void add(Item item) {
        items.add(item);
        save();
    }

    public List<Item> all() {
        return items;
    }

    public Item findById(int id) {
        for (Item it : items) {
            if (it.getId() == id) {
                return it;
            }
        }
        return null;
    }

    public List<Item> byCategory(Category category) {
        List<Item> result = new ArrayList<>();
        for (Item it : items) {
            if (it.getCategory() == category) {
                result.add(it);
            }
        }
        return result;
    }
}
