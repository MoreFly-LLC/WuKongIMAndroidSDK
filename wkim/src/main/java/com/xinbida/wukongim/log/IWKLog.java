package com.xinbida.wukongim.log;

import androidx.annotation.NonNull;

public interface IWKLog {

    void d(@NonNull String tag, @NonNull String msg);

    void i(@NonNull String tag, @NonNull String msg);

    void w(@NonNull String tag, @NonNull String msg);

    void e(@NonNull String tag, @NonNull String msg);

}
