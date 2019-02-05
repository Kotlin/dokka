package bar;

/**
 * Just a fruit
 */
public class Banana {
    private Double weight;

    /**
     * Returns weight
     *
     * @return weight
     * @deprecated
     */
    public Double getWeight() {
        return weight;
    }

    /**
     * Sets weight
     *
     * @param weight in grams
     * @deprecated with message
     */
    public void setWeight(Double weight) {
        this.weight = weight;
    }
}