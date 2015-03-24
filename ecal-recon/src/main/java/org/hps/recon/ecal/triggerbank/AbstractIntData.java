package org.hps.recon.ecal.triggerbank;

import java.util.Arrays;
import org.lcsim.event.GenericObject;

/**
 * GenericObject representation of an INT32/UINT32 bank read from EVIO. The bank
 * header tag identifies the type of data, and is stored as the first int in the
 * GenericObject. The contents of the bank are the remaining N-1 ints.
 * Constructors are provided from int[] (for reading from EVIO) and from
 * GenericObject (for reading from LCIO).
 *
 * Subclasses must implement the two constructors and two abstract methods, plus
 * whatever methods are needed to access the parsed data.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public abstract class AbstractIntData implements GenericObject {

    protected int[] bank;

    /**
     * Constructor from EVIO int bank. Bank tag must be checked by EVIO reader
     * before the constructor is called.
     *
     * @param bank
     */
    protected AbstractIntData(int[] bank) {
        if (bank == null) {
            this.bank = new int[0];
        } else {
            this.bank = Arrays.copyOf(bank, bank.length);
        }
    }

    /**
     * Constructor from LCIO GenericObject. Checks the bank tag; subclass
     * constructor must set the expected value of the tag.
     *
     * @param data
     * @param expectedTag
     */
    protected AbstractIntData(GenericObject data, int expectedTag) {
        if (getTag(data) != expectedTag) {
            throw new RuntimeException("expected tag " + expectedTag + ", got " + getTag(data));
        }
        this.bank = getBank(data);
    }

    public int[] getBank() {
        return bank;
    }

    /**
     * Return the int bank of an AbstractIntData read from LCIO.
     *
     * @param object
     * @return
     */
    public static int[] getBank(GenericObject object) {
        int N = object.getNInt() - 1;
        int[] bank = new int[N];
        for (int i = 0; i < N; i++) {
            bank[i] = object.getIntVal(i + 1);
        }
        return bank;
    }

    /**
     * Return a single value from the int bank of an AbstractIntData.
     *
     * @param object
     * @param index
     * @return
     */
    public static int getBankInt(GenericObject object, int index) {
        return object.getIntVal(index + 1);
    }

    /**
     * Returns the EVIO bank header tag expected for this data.
     *
     * @return
     */
    public abstract int getTag();

    /**
     * Returns the EVIO bank tag for a data object.
     *
     * @param data
     * @return
     */
    public static int getTag(GenericObject data) {
        return data.getIntVal(0);
    }

    /**
     * Parses the bank so the object can be used in analysis.
     */
    protected abstract void decodeData();

    @Override
    public int getNInt() {
        return bank.length + 1;
    }

    @Override
    public int getNFloat() {
        return 0;
    }

    @Override
    public int getNDouble() {
        return 0;
    }

    @Override
    public int getIntVal(int index) {
        if (index == 0) {
            return getTag();
        }
        return bank[index - 1];
    }

    @Override
    public float getFloatVal(int index) {
        throw new UnsupportedOperationException("No float values in " + this.getClass().getSimpleName());
    }

    @Override
    public double getDoubleVal(int index) {
        throw new UnsupportedOperationException("No double values in " + this.getClass().getSimpleName());
    }

    @Override
    public boolean isFixedSize() {
        return true;
    }
}