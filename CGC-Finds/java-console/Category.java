/**
 * The fixed list of item categories. Searching by category uses this enum.
 */
public enum Category {
    ELECTRONICS("Electronics"),
    DOCUMENTS("Documents / ID cards"),
    WALLETS_BAGS("Wallets & Bags"),
    KEYS("Keys"),
    CLOTHING("Clothing & Accessories"),
    JEWELLERY("Jewellery & Watches"),
    BOOKS("Books & Stationery"),
    OTHER("Other");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
