package toniarts.openkeeper.tools.convert;

import toniarts.openkeeper.tools.convert.kmf.KmfFile;
import toniarts.openkeeper.tools.convert.kmf.Material;
import toniarts.openkeeper.utils.PathUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.logging.Logger;

/**
 * Created by wietse on 11/04/17.
 */
public class KmfFileWalker {

    private static String dkIIFolder;
    private static String kmfFolder;
    private static final Logger logger = Logger.getLogger(KmfFileWalker.class.getName());

    public static void main(String[] args) {
        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 2 || !new File(args[1]).exists()) {
            dkIIFolder = PathUtils.getDKIIFolder();
            if (dkIIFolder == null)
            {
                throw new RuntimeException("Please provide file path to the KMF directory as a first parameter! Second parameter is the Dungeon Keeper II main folder (optional)");
            }
        } else {
            dkIIFolder = PathUtils.fixFilePath(args[1]);
        }
        if(args.length > 0 && new File(args[0]).exists()) {
            try{
                final PrintWriter writer = new PrintWriter("material_flags.txt", "UTF-8");
                kmfFolder = args[0];
                final File[] files = find(kmfFolder, "kmf");
                for(File file : files) {
                    KmfFile kmfFile = new KmfFile(file);
                    if(kmfFile.getMaterials() != null && !kmfFile.getMaterials().isEmpty()) {
                        writer.println(file.getName());
                        for(Material material : kmfFile.getMaterials()) {
                            if(material.getFlag() != null && !material.getFlag().isEmpty()) {
                                String flags = "";
                                for(Material.MaterialFlag mf : material.getFlag()) {
                                    flags += mf.name() + " " + mf.getFlagValue() + " ";
                                }
                                writer.println("  " + material.getName() + " " + flags);
                            }
                        }
                    }
                }

                writer.close();

            } catch (IOException e) {
                logger.info(e.getMessage());
            }

        }
    }



    public static File[] find( String dirName, String extension){
        File dir = new File(dirName);

        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename)
            { return filename.endsWith("." + extension); }
        } );

    }


}
