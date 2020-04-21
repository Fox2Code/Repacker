package com.fox2code.repacker.tests;

import com.fox2code.repacker.utils.Utils;
import org.junit.Test;

public class Tests {
    @Test
    public void testCountParmsOnSignature() {
        int r;
        if (1 != (r = Utils.countParms("(Ljava/lang/String<Ljava/lang/String;>;)Ljava/lang/String;"))) {
            throw new Error("testCountParmsOnSignature countParms result is not 1 (got "+r+")");
        }
    }

    @Test
    public void testCountParmsOnGenericSignature() {
        int r;
        if (1 != (r = Utils.countParms("<T:Ljava/lang/Object;>([TT;)Ljava/util/List<TT;>;"))) {
            throw new Error("testCountParmsOnGenericSignature countParms result is not 1 (got "+r+")");
        }
    }

    @Test
    public void testCountParmsCompare() {
        int r1, r2;
        if ((r1 = Utils.countParms("(Ljava/lang/String;)Ljava/lang/String;")) !=
                (r2 = Utils.countParms("(Ljava/lang/String<Ljava/lang/String;>;)Ljava/lang/String;"))) {
            throw new Error("testCountParmsCompare failed ("+r1+" != "+r2+")");
        }
    }
}
