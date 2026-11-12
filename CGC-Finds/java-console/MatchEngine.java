import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The unique feature: suggests matching LOST and FOUND reports using keywords.
 *
 * How it works:
 *   1. Turn the item's name + description into a set of keywords
 *      (lowercase, remove filler words like "the", treat "mobile" = "phone", etc.).
 *   2. Compare the keywords of a LOST report with a FOUND report.
 *   3. Score = keyword overlap (up to 70) + same category (20) + same place word (10).
 *   4. Show the matches with a score of 30 or more, best first.
 */
public class MatchEngine {

    /** One suggested match. */
    public static class Match {
        public final Item item;
        public final int score;
        public final Set<String> sharedKeywords;

        Match(Item item, int score, Set<String> sharedKeywords) {
            this.item = item;
            this.score = score;
            this.sharedKeywords = sharedKeywords;
        }
    }

    private static final int MIN_SCORE = 30;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "in", "on", "at", "to", "for", "with",
            "my", "his", "her", "its", "our", "is", "was", "it", "this", "that", "has",
            "have", "had", "near", "by", "from", "very", "some", "one", "lost", "found",
            "item", "small", "big", "large");

    /** Different words that mean the same thing are mapped to one word. */
    private static final Map<String, String> SYNONYMS = new HashMap<>();
    static {
        for (String w : new String[]{"mobile", "cellphone", "smartphone", "iphone", "phone"}) SYNONYMS.put(w, "phone");
        for (String w : new String[]{"bag", "backpack", "rucksack", "handbag", "satchel"}) SYNONYMS.put(w, "bag");
        for (String w : new String[]{"wallet", "purse", "billfold"}) SYNONYMS.put(w, "wallet");
        for (String w : new String[]{"laptop", "notebook", "macbook"}) SYNONYMS.put(w, "laptop");
        for (String w : new String[]{"earphone", "earbud", "headphone", "airpod", "headset"}) SYNONYMS.put(w, "earphone");
        for (String w : new String[]{"id", "identity", "aadhar", "aadhaar", "licence", "license"}) SYNONYMS.put(w, "idcard");
        for (String w : new String[]{"spectacle", "glass", "glasse", "eyeglass", "eyeglasse", "specs"}) SYNONYMS.put(w, "glasses");
        for (String w : new String[]{"umbrella", "brolly"}) SYNONYMS.put(w, "umbrella");
        for (String w : new String[]{"watch", "wristwatch"}) SYNONYMS.put(w, "watch");
        for (String w : new String[]{"bottle", "flask"}) SYNONYMS.put(w, "bottle");
        for (String w : new String[]{"key", "keychain", "keyring"}) SYNONYMS.put(w, "key");
    }

    /** Break text into a clean set of keywords. */
    public static Set<String> keywords(String text) {
        Set<String> result = new TreeSet<>();
        for (String raw : text.toLowerCase().split("[^a-z0-9]+")) {
            if (raw.isEmpty() || STOP_WORDS.contains(raw)) {
                continue;
            }
            String w = raw;
            // very simple plural handling: "keys" -> "key", but keep "glass", "bus"
            if (w.length() > 3 && w.endsWith("s") && !w.endsWith("ss")) {
                w = w.substring(0, w.length() - 1);
            }
            w = SYNONYMS.getOrDefault(w, w);
            if (!STOP_WORDS.contains(w) && w.length() > 1) {
                result.add(w);
            }
        }
        return result;
    }

    private static Set<String> itemKeywords(Item it) {
        return keywords(it.getName() + " " + it.getDescription());
    }

    /** Find the best opposite-type reports for the given item. */
    public static List<Match> suggest(Item target, List<Item> all) {
        List<Match> matches = new ArrayList<>();
        Set<String> targetWords = itemKeywords(target);
        Set<String> targetPlace = keywords(target.getLocation());

        for (Item other : all) {
            boolean oppositeType = other.getType() != target.getType();
            if (!oppositeType || other.isResolved() || other.getId() == target.getId()) {
                continue;
            }

            Set<String> otherWords = itemKeywords(other);
            Set<String> shared = new TreeSet<>(targetWords);
            shared.retainAll(otherWords);
            if (shared.isEmpty()) {
                continue; // no common keyword = not a match
            }

            // Dice coefficient: 2*shared / (sizeA + sizeB), scaled to 70 points
            double dice = 2.0 * shared.size() / (targetWords.size() + otherWords.size());
            int score = (int) Math.round(dice * 70);

            if (other.getCategory() == target.getCategory()) {
                score += 20;
            }

            Set<String> placeShared = new HashSet<>(targetPlace);
            placeShared.retainAll(keywords(other.getLocation()));
            if (!placeShared.isEmpty()) {
                score += 10;
            }

            score = Math.min(score, 100);
            if (score >= MIN_SCORE) {
                matches.add(new Match(other, score, shared));
            }
        }
        matches.sort(Comparator.comparingInt((Match m) -> m.score).reversed());
        return matches;
    }
}
