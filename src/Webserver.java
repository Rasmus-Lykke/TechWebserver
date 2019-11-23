import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;


public class Webserver implements Runnable{

    private static final File WEB_ROOT = new File(".");
    private static final String DEFAULT_FILE = "index.html";
    private static final String FILE_NOT_FOUND = "404.html";
    private static final String METHOD_NOT_SUPPORTED = "not_supported.html";

    // port to listen connection
    private static final int PORT = 8009;

    // verbose mode
    private static final boolean accepted = true;

    // Client Connection via Socket Class
    private Socket connect;

    private Webserver(Socket socket) {
        connect = socket;
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port : " + PORT + "\n");

            // we listen until user halts execution
            while (true) {
                Webserver server = new Webserver(serverConnect.accept());

                if (accepted) {
                    System.out.println("\n\nConnecton opened. (" + new Date() + ")");
                }

                // thread to manage each client...
                Thread thread = new Thread(server);
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }

    @Override
    public void run() {

        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;


        try {
            while(true){
            // read characters from the client via input stream on the socket
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));

            // get character output stream to client (for headers)
            out = new PrintWriter(connect.getOutputStream());

            // get binary output stream to client (for requested data)
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            // get first line of the request from the client
            String input = in.readLine(); // todo --> Lav tjek om objektet ikke er null

            //parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input); // todo --> connection dør her ???
            String HttpMethod = parse.nextToken().toUpperCase();
            //get file requested
            fileRequested = parse.nextToken().toLowerCase();

            // check if httpmethod is either get or head
            if (!HttpMethod.equals("GET")  &&  !HttpMethod.equals("HEAD")) {
                if (accepted) {
                    System.out.println("501 Not Implemented : " + HttpMethod + " HttpMethod.");
                }

                // return the not supported file to the client
                File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
                int fileLength = (int) file.length();
                String contentType = "text/html";
                //read content to return to client
                byte[] fileData = readFileData(file, fileLength);

                //send HTTP Headers with data to client
                out.println("HTTP/1.1 501 Not Implemented");
                out.println("Server: Java HTTP Server: 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + contentType);
                out.println("Content-length: " + fileLength);
                out.println(); // blank line between headers and content
                out.flush(); // flush character output stream buffer

                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();

            } else {
                // GET or HEAD HttpMethod
                if (fileRequested.endsWith("/")) { //if non other requested - send default file
                    fileRequested += DEFAULT_FILE;
                }

                File file = new File(WEB_ROOT, fileRequested);
                int fileLength = (int) file.length();
                String content = getContentType(fileRequested);

                if (HttpMethod.equals("GET")) { // GET HttpMethod so we return content
                    byte[] fileData = readFileData(file, fileLength);

                    // send HTTP Headers
                    System.out.println("HTTP/1.1 200 OK");
                    System.out.println("Date: " + new Date());
                    System.out.println("Content-type: " + content);
                    System.out.println("Content-size: " +fileLength);
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Java HTTP Server: 1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-type: " + content);
                    out.println("Content-length: " + fileLength);
                    out.println(); // blank line between headers and content, very important !
                    out.flush(); // flush character output stream buffer

                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
                }

                if (accepted) {
                    System.out.println("File " + fileRequested + " of type " + content + " returned");
                }

            }
        }

        } catch (FileNotFoundException ex) {
            try {
                fileNotFound(out, dataOut, fileRequested);
            } catch (IOException ioe) {
                System.err.println("Error with file not found exception : " + ioe.getMessage());
            }

        } catch (IOException ioe) {
            System.err.println("Server error : " + ioe);
        } finally {
            try { //close as you go..
                in.close();
                out.close();
                dataOut.close();
                connect.close(); // we close socket connection
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }

            if (accepted) {
                System.out.println("Connection closed.\n");
            }
        }
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }

    // return supported Types
    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html")) {
            return "text/html";
        } else if(fileRequested.endsWith(".jpg") || fileRequested.endsWith(".png") || fileRequested.endsWith(".jpeg") || fileRequested.endsWith(".bmp")){
            return "image";
        } else if(fileRequested.endsWith(".pdf") || fileRequested.endsWith(".application") || fileRequested.endsWith(".msword")){
            return "pdf/application/msword";
        }
        else{
            return "text/plain";
        }
    }


    private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
        int fileLength = (int) file.length();
        String content = "text/html";
        byte[] fileData = readFileData(file, fileLength);

        out.println("HTTP/1.1 404 File Not Found");
        out.println("Server: Java HTTP Server: 1.0");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer

        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();


        if (accepted) {
            System.out.println("File " + fileRequested + " not found");
        }
    }

}