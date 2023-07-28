package cc.calliope.mini.utils;

import android.os.Build;

public class Version {
    public static final boolean upperMarshmallow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    public static final boolean upperNougat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    public static final boolean upperOreo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    public static final boolean upperQuinceTart = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    public static final boolean upperSnowCone = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    public static final boolean upperTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
}
