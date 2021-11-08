package xmlutils;


import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.Random;


public interface XMLfuncs {

    static ArrayList<String> createBrokenXML(String XML, int size) {
        ArrayList<String> lista = new ArrayList<>();
        int nparts = (int) Math.ceil((double) XML.length() / (double) size);
        int leftLimit = '0';
        int rightLimit = 'z';
        int targetStringLength = 8;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        String generatedString = buffer.toString();

        for (int i = 0; i < nparts; i++) {
            JSONObject jsonobject = new JSONObject();
            jsonobject.put("id", generatedString);
            jsonobject.put("nPart", i);
            jsonobject.put("totalParts", nparts);
            int start = i * size;
            int end = Math.min(start + size, XML.length());
            jsonobject.put("xmlPart", XML.substring(start, end));
            lista.add(jsonobject.toString());
        }
        return lista;
    }

    static void write_xml_to_file(String fileName, String str)
            throws IOException {
        FileOutputStream outputStream = new FileOutputStream(fileName);
        byte[] strToBytes = str.getBytes();
        outputStream.write(strToBytes);
        outputStream.close();
    }

    static String read_xml_from_file(String fileName)
            throws IOException {
        String contents="";
        contents = new String(Files.readAllBytes(Paths.get(fileName)));


        return contents;
    }

    static String get_xml_from_server(String serverUrl)
    {
        String s = "";
        StringBuffer textoxml=new StringBuffer();
        try
        {
            URL url;
            URLConnection urlConn;
            DataInputStream dis;

            url = new URL(serverUrl);

            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);

            dis= new DataInputStream(urlConn.getInputStream());
            while((s=dis.readLine())!=null){
                textoxml.append(s);
            }
            dis.close();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return textoxml.toString();
    }
}