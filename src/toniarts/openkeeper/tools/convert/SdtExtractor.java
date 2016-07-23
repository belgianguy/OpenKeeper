/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import toniarts.openkeeper.constants.DkIIFolderConstants;
import toniarts.openkeeper.tools.convert.sound.SdtFile;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Simple class to extract all the files from given SDT to given location
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SdtExtractor {

    private static String dkIIFolder;

    public static void main(String[] args) throws IOException {

        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 2 || !new File(args[1]).exists()) {
            dkIIFolder = PathUtils.getDKIIFolder();
            if (dkIIFolder == null || args.length == 0)
            {
                throw new RuntimeException("Please provide extraction target folder as a first parameter! Second parameter is the Dungeon Keeper II main folder (optional)!");
            }
        } else {
            dkIIFolder = PathUtils.fixFilePath(args[1]);
        }

        final String soundFolder = dkIIFolder.concat(DkIIFolderConstants.DKII_DATA_FOLDER).concat(File.separator).concat(DkIIFolderConstants.DKII_SOUND_FOLDER).concat(File.separator).concat(DkIIFolderConstants.DKII_SFX_FOLDER).concat(File.separator);

        //And the destination
        String destination = PathUtils.fixFilePath(args[0]);

        //Find all the sound files
        final List<File> sdtFiles = new ArrayList<>();
        File dataDir = new File(soundFolder);
        Files.walkFileTree(dataDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                //Get all the SDT files
                if (attrs.isRegularFile() && file.getFileName().toString().toLowerCase().endsWith(".sdt")) {
                    sdtFiles.add(file.toFile());
                }

                //Always continue
                return FileVisitResult.CONTINUE;
            }
        });

        //Extract the sounds
        for (File file : sdtFiles) {
            SdtFile sdt = new SdtFile(file);

            //Get a relative path
            Path relative = dataDir.toPath().relativize(file.toPath());
            String dest = destination;
            dest += relative.toString();

            //Remove the actual file name
            dest = dest.substring(0, dest.length() - file.toPath().getFileName().toString().length());

            //Extract
            sdt.extractFileData(dest);
        }
    }
}
