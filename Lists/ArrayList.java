package Lists;

import java.util.Arrays;
import java.util.function.Predicate;

public class ArrayList<T> {
    private T[] data;
    private int left; // pointer to the available (or empty) slot on the left of the array. _lfs is an
                      // abreviation for _leftFreeSlot
    private int right; // pointer to the available (or empty) slot on the right of the array. _rfs is
                      // an abreviation for _rightFreeSlot
    private ArrayListManager usageManager; // check the internal array (_array) usage to optimize its memory usage
    private int size;
    private static final int DEFAULT_CAPACITY = 50;
    private int _capacity = 0;
    private boolean fixedCapacity = false;

    public int size(){
        return size;
    }

    //
    //
    // CONSTRAINTS
    //
    // ################################################################################

    // Build a new empty ArrayList
    public ArrayList() {
        setSlotPointers();
        usageManager = new ArrayListManager(this);
        data = createGenericArray(DEFAULT_CAPACITY);
    }

    // Build a new empty ArrayList with a spcecified initial capacity and posibility
    // of fixed capacity
    public ArrayList(int initialCapacity, boolean fixedCapacity) {
        setSlotPointers();
        usageManager = new ArrayListManager(this);
        data = createGenericArray(initialCapacity);
        this.fixedCapacity = fixedCapacity;
    }

    // Build a new ArrayList of an array with the posibility of fixed capacity
    public ArrayList(boolean fixedCapacity, @SuppressWarnings("unchecked") T... items) {
        right = data.length; // asume that array contains not null items
        left = -1;
        usageManager = new ArrayListManager(this);
        this.fixedCapacity = fixedCapacity;
    }

    @SuppressWarnings("unchecked")
    private T[] createGenericArray(int size) {
        return (T[]) new Object[size];
    }

    //
    //
    // INSERTION METHODS
    //
    // ################################################################################

    public boolean add(T item) {
        return append(item);
    }

    public boolean prepend(T value) { // Add a new element to the head of this arraylist
        usageManager.checkExpandable();
        usageManager.checkIsFullAtLeft();
        data[left] = value;
        left--;
        size++;
        return true;
    }

    public boolean append(T value) { // Add a new element to the end (tail) of this arraylist
        usageManager.checkExpandable();
        usageManager.checkIsFullAtRight();
        data[right] = value;
        right++;
        size++;
        return true;
    }

    public boolean insert(int i, T value) // Insert a new element in a specified index
    {
        if (i < 0 || i > size)
            throw new IndexOutOfBoundsException("Index out of range: " + i);
        usageManager.checkExpandable();

        if (i == 0)
            return prepend(value);
        else if (i == size)
            return append(value);

        // At this point the internal array can either be filled only on one side (left
        // or right, but not both), or be free on both sides.
        // (see MemoryManager.CheckCapacity(bool))

        if (left == -1) { // Is full at left
            System.arraycopy(data, i, data, i + 1, size - i); // move all elements from the ith onwards one slot
                                                                 // to the right
            right++;
        } else { // is full at right
            System.arraycopy(data, 1, data, 0, left + i + 1); // move the first 'i' elements one slot to the left
            left--;
        }
        data[i] = value;
        size++;
        return true;
    }

    //
    //
    // REMOVAL METHODS
    //
    // ################################################################################

    public boolean removeAt(int i) // Remove the i-th element of this list
    {
        if (isEmpty()) {
            return false;
        }
        checkOutOfRange(i);
        if (i == 0)
            return removeHead();
        if (i == size - 1)
            return removeTail();

        /*
         * In this section it identifies two possible cases.
         */

        if (i <= (size / 2)) { // i is in the left middle of this ArrayList
            System.arraycopy(data, internalIndexOf(0), data, internalIndexOf(1), i); // Move the first 'i' elements to the
                                                                                 // right
            left++;
        } else { // i is the right middle of this ArrayList
            System.arraycopy(data, internalIndexOf(i + 1), data, internalIndexOf(i), right - internalIndexOf(i + 1)); // Move
            right--;
        }
        size--;
        usageManager.checkReducible();
        checkSlotPointers();
        return true;
    }

    public boolean remove(T value) { // Remove ONLY THE FIRST element that matches the specified value of this list
        int indexToRemove = -1;
        for (int i = 0; i < size; i++) {
            if (get(i).equals(value)) {
                indexToRemove = i;
                break;
            }
        }
        return indexToRemove >= 0 && removeAt(indexToRemove);
    }

    public boolean removeAll(Predicate<T> predicate) // remove ALL elements that matches the predicate
    {
        if (isEmpty()) {
            return false;
        }
        boolean removed = false;
        for (int i = 0; i < size; i++) // can be simplified or optimized
        {
            while (!isEmpty() && i < size && predicate.test(data[internalIndexOf(i)])) {
                removed = removeAt(i);
            }
        }
        return removed;
    }

    public boolean removeHead() // Remove the head; the first element
    {
        if (isEmpty()) {
            return false;
        }
        left++;
        size--;
        usageManager.checkReducible();
        checkSlotPointers();
        return true;
    }

    public boolean removeTail() // Remove the tail; the last element
    {
        if (isEmpty()) {
            return false;
        }
        right--;
        size--;
        usageManager.checkReducible();
        checkSlotPointers();
        return true;
    }

    public boolean removeIf(Predicate<T> predicate) { // remove ONLY THE FIRST element that satisfies the predicate

        // int indexToRemove = Enumerable.Range(0, Count).FirstOrDefault(i => this[i] !=
        // null && predicate(this[i]));
        int indexToRemove = -1;
        for (int i = 0; i < size; i++) {
            if (predicate.test(get(i))) { // The current element satisfies the predicate
                indexToRemove = i;
                break;
            }
        }
        return indexToRemove >= 0 && removeAt(indexToRemove);
    }

    //
    //
    // REPLACEMENT METHODS
    //
    //
    // ################################################################################

    // Replace ONLY THE FIRST element that match the predicate for a new value
    public boolean replaceIf(Predicate<T> predicate, T newValue) {
        int indexToReplace = -1;
        for (int i = 0; i < size; i++) {
            if (predicate.test(get(i))) {
                indexToReplace = i;
                break;
            }
        }

        if (indexToReplace >= 0) {
            set(indexToReplace, newValue);
        }
        return indexToReplace >= 0;
    }

    // Replace all elements that match the predicate for a new value
    public boolean replaceAll(Predicate<T> predicate, T newValue) {
        boolean replaced = false;
        for (int i = 0; i < size; i++) {
            if (predicate.test(get(i))) {
                set(i, newValue);
                replaced = true;
            }
        }
        return replaced;
    }

    public T get(int index) {
        return data[internalIndexOf(index)];
    }

    private void set(int index, T value) {
        data[internalIndexOf(index)] = value;
    }

    /**
     * Translates a logical index used by this ArrayList to the corresponding
     * actual index in the internal array storage.
     * 
     * This method allows determining the real position of the ith element within
     * the internal array without the need to calculate it manually.
     * 
     * @param logicalIndex - The index that it want to translate
     * @return The internal index
     */
    private int internalIndexOf(int logicalIndex) {
        return left + logicalIndex + 1; // The i-th element of this ArrayList is actually at (_lfs + i + 1) internally
    }

    //
    //
    // EXTRACTION METHODS
    //
    // ################################################################################

    public T extractHead() // Remove and return the head of this ArrayList (the first element)
    {
        T head = get(0);
        left++;
        size--;
        return head;
    }

    public T extractTail() // Remove and return the tail of this ArrayList (the last element)
    {
        T tail = get(size - 1);
        right--;
        size--;
        return tail;
    }

    public T extract(int i) // Remove and return the i-th element
    {
        T ielement = get(i);
        removeAt(i);
        return ielement;
    }

    //
    //
    // QUERIES AND UTILITIES
    //
    // ################################################################################

    public boolean isEmpty() { // return true if this ArrayList have no elements
        return size == 0;
    }

    public void sort() {
        sort(false);
    }

    public void sort(boolean descending) {
        if (descending) // if descending == true, reverse the order
            Arrays.sort(data, internalIndexOf(0), size);
        else
            Arrays.sort(data, internalIndexOf(0), size);
    }

    // reverse the order of this ArrayList
    public void reverse() {
        for (int i = 0, j = size - 1; i < j; i++, j--) {
            T temp = get(i);
            set(i, get(j));
            set(j, temp);
        }
    }

    public void clear() // clear this ArrayList (remove all elements). It can also be seen as a resetter
    {
        data = createGenericArray(DEFAULT_CAPACITY);
        _capacity = DEFAULT_CAPACITY;
        size = 0;
        setSlotPointers();
    }

    // Get the index of the first match with the given value
    // Example:
    // list = [10,5,3,7,6,1];
    // Console.WriteLine(IndexOf(7)); // out = 3
    // Console.WriteLine(IndexOf(10)); // out = 0
    public int indexOf(T value) {
        int cont = 0;
        for (T current : data)
            if (current.equals(value))
                return cont;
        return 0;
    }

    // get the Sub-List from a startIndex (inclusive) to the end of this ArrayList
    // For example:
    // list = [1,2,3,4,5]
    // list.SuList(1) = [2,3,4,5]
    // list.SubList(3) = [4,5]
    public ArrayList<T> subList(int startIndex) {
        return subList(startIndex, size - 1);
    }

    /**
     * get the Sub-List from a startIndex to endIndex (both inclusive)
     * <p>
     * For example:
     * list = [1,2,3,4,5]
     * list.SubList(1,3) = [2,3,4]
     * list.SubLIst(0,2) = [1,2,3]
     */
    public ArrayList<T> subList(int startIndex, int endIndex) {
        checkOutOfRange(startIndex, endIndex);
        if (startIndex > endIndex) {
            throw new IllegalArgumentException("Start index can't be greater than end index");
        }
        // return new(this.Skip(startIndex).Take(endIndex - startIndex + 1).ToArray());
        ArrayList<T> subList = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            subList.add(get(i));
        }
        return subList;
    }

    // Get a sub-list with all elements that match the predicate
    public ArrayList<T> filter(Predicate<T> predicate) {
        // new(this.Where(n => predicate(n)).ToArray()); // Get a sub-list of all
        // elements that satisfy the predicate
        ArrayList<T> filtered = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            T current = get(i);
            if (predicate.test(current)) {
                filtered.add(current);
            }
        }
        return filtered;
    }

    // Concatenate this ArrayList with another ArrayList.
    public void concat(ArrayList<T> otherArrayList) {
        int oCount = otherArrayList.size; // size of the other list
        int rightSpaceAvailable = _capacity - right; // available space at right of internal array
        T[] oData = otherArrayList.data; // the other internal array

        if (oCount <= rightSpaceAvailable) { // new elements fit on the right
            System.arraycopy(oData, otherArrayList.left + 1, data, right, oCount); // Copy all elements of the other
                                                                                   // arrayList
            // to the right of this internal array
            // Update fields
            right += oCount;
            size += oCount;

        } else { // O (n + m);
            T[] newArray = createGenericArray(size + oCount); // the new array
            System.arraycopy(data, left + 1, newArray, 0, size); // Copy all elements of this at new array
            System.arraycopy(oData, otherArrayList.left + 1, newArray, size, oCount); // Concat all elements of other
                                                                                       // at
            // new array
            // Update fields
            size += oCount;
            right = size;
            left = -1;
            data = newArray;
            _capacity = data.length;
        }
    }

    // public static ArrayList<T> Concat(ArrayList<T> list1, ArrayList<T> list2)
    // {
    // ArrayList<T> result = new([.. list1]);
    // result.Concat(list2);
    // return result;
    // }

    // Return a simple representation of this list in an array format like this:
    // [a1, a2, a3, ..., a_n]
    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        StringBuilder str = new StringBuilder("[" + get(0));
        for (int i = 1; i < size; i++) {
            str.append(",").append(get(i));
        }
        return str.append("]").toString();
    }

    //
    //
    //
    // CHECK METHODS
    //
    //

    void checkOutOfRange(int... indexes) { // Throw an exception if any given index is out of range
        if (Arrays.stream(indexes).anyMatch(i -> i < 0 || i >= size)) {
            throw new IndexOutOfBoundsException("Index out of range");
        }
    }

    void checkSlotPointers() { // Reset the slots pointers (_leftFreeSlot and _rightFreeSlot) in the center of
                               // intern Array if this ArrayList is empty
        // This method is used to prevent pointers from overlapping or crossing when
        // this ArrayList is emptied after successive deletions
        if (isEmpty()) {
            setSlotPointers();
        }
    }

    void setSlotPointers() { // Set the slot pointers in the center of the intern array (_data)
        right = _capacity / 2;
        left = right - 1;
    }

    //
    //
    // MEMORY MANAGER
    //
    // A class to manage the internal use of array
    // ################################################################################

    private class ArrayListManager {
        private ArrayList<T> list;
        private final int EXPAND_FACTOR = 2;
        private final float REDUCTION_FACTOR = 0.5f;

        public ArrayListManager(ArrayList<T> arrayList) {
            this.list = arrayList;
        }

        public void checkIsFullAtLeft() {   // Check if the inter array of the list is full at left
            if (isFullAtLeft()) {
                shiftLeftToCenter(); // Move all elements from left to center of the intern array
            }
        }

        public void checkIsFullAtRight() {  // Check if the inter array of the list is full at right
            if (isFullAtRight()) {
                shiftRightToCenter(); // Move all elements from right to center of the intern array
            }
        }

        void shiftLeftToCenter() {   // Move all elements from left to center of the intern array
            int shift = (int) Math.ceil((double) (list._capacity - list.size) / 2);
            System.arraycopy(list.data, 0, list.data, shift, list.size);
            list.left += shift;
            list.right += shift;
        }

        void shiftRightToCenter() {   // Move all elements from right to center of the intern array
            int shift = (int) Math.ceil((double) (list._capacity - list.size) / 2);
            System.arraycopy(list.data, list.left + 1, list.data, list.left + 1 - shift, list.size);
            list.left -= shift;
            list.right -= shift;
        }

        boolean isFullAtRight() {
            return list.right == list._capacity;
        }

        boolean isFullAtLeft() {
            return list.left == -1;
        }

        public void checkExpandable() { // if the internArray is expandible, expand it
            if (list.size == list._capacity) { // the ArrayList is full
                if (list.fixedCapacity) { // the capacity of the arraylist is fixed ==> it can't add a new element
                    return;
                } else {
                    expand();
                }
            }
        }

        protected void expand() {   // Expand the intern array
            int increment = list._capacity / 2;
            T[] newArr = createGenericArray(list._capacity * EXPAND_FACTOR);
            System.arraycopy(list.data, 0, newArr, increment, list.size);
            list.data = newArr;
            list._capacity = list.data.length;
            list.left += increment;
            list.right += increment;
        }

        public void checkReducible() {   // if the intern array is reducible, reduce it
            if (!list.fixedCapacity && list.size <= (int) (list._capacity * REDUCTION_FACTOR)
                    && list._capacity > DEFAULT_CAPACITY) {
                reduce();
            }
        }

        protected void reduce() {   // Reduce the intern array
            if (list.fixedCapacity) {
                return;
            }
            T[] newArray = createGenericArray((int) (list._capacity * REDUCTION_FACTOR));
            for (int i = 0, j = list.left + i + 1; i < newArray.length; i++, j++) { // Could be replaced by System.arraycopy()
                newArray[i] = list.data[j];
            }
            list.data = newArray;
            list._capacity = list.data.length;
            list.left = -1;
            list.right = list.size;
        }
    }
}