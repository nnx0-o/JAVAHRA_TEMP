class Array<E> {
    private E[] items;
    private int itemCount;

    public Array(int size) {
        this.items = (E[])(new Object[size]);
        this.itemCount = 0;
    }

    public boolean addItem(E newItem) {
        if (this.itemCount == this.items.length)
            return false;
        this.items[this.itemCount++] = newItem;
        return true;
    }

    public E getItem(int index) {
        if (index >= this.itemCount) return null;
        return this.items[index];
    }

    public E removeItem(int index) {
        if (index >= this.itemCount) return null;
        E removedItem = this.items[index];
        for (int i=index; i<this.itemCount-1; i++) {
            this.items[i] = this.items[i+1];
        }
        this.items[--this.itemCount] = null;
        return removedItem;
    }

    @Override
    public String toString() {
        String tisk = "Polozky\n";
        for (int i=0; i<this.itemCount; i++) {
            tisk += this.items[i] + "\n";
        }
        return tisk;
    }

}