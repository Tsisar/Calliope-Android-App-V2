package cc.calliope.mini_v2.utils;

import android.os.Build;

public class Version {
    public static final boolean upperKitkat = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT;
    public static final boolean upperMarshmallow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    public static final boolean upperNougat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    public static final boolean upperOreo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    public static final boolean upperQuinceTart = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    public static final boolean upperSnowCone = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
}
