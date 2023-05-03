package ir.sharif.math.ap2023.hw5;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Writer extends RandomAccessFile{
    public Writer(File file,long size) throws IOException {
        super(file, "rw");
        // file in progress test
        setLength(size);
    }
}
