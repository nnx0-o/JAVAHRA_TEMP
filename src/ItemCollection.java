public class ItemCollection<E> {
    private E[] items;
    private int itemCount;

    public ItemCollection(int size) {
        this.items = (E[])(new Object[size]);
        this.itemCount = 0;
    }

    public boolean addItem(E newItem) {
        if (newItem == null || this.itemCount == this.items.length)
            return false;
        this.items[this.itemCount++] = newItem;
        return true;
    }

    public E getItem(int index) {
        if (!containsIndex(index)) return null;
        return this.items[index];
    }

    public E removeItem(int index) {
        if (!containsIndex(index)) return null;
        E removedItem = this.items[index];
        for (int i=index; i<this.itemCount-1; i++) {
            this.items[i] = this.items[i+1];
        }
        this.items[--this.itemCount] = null;
        return removedItem;
    }

    public int size() {
        return itemCount;
    }

    public boolean isFull() {
        return itemCount == items.length;
    }

    private boolean containsIndex(int index) {
        return index >= 0 && index < itemCount;
    }

    @Override
    public String toString() {
        String output = "Polozky\n";
        for (int i=0; i<this.itemCount; i++) {
            output += this.items[i] + "\n";
        }
        return output;
    }

}
