package org.osmaintenance.scripts;

import com.sun.jna.*;
import com.sun.jna.win32.*;
public interface WindowsNativeAPI extends StdCallLibrary {
    WindowsNativeAPI INSTANCE = Native.load("kernel32", WindowsNativeAPI.class, W32APIOptions.DEFAULT_OPTIONS);

    boolean MoveFileExW(WString existingFileName, WString newFileName, int flags);
}
