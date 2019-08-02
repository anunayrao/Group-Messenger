package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.SerializablePermission;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.System.out;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    //static final String[] REMOTE_PORT = {"11108","11112","11116","11120","11124"};
    static ArrayList<String> REMOTE_PORT = new ArrayList<String>();


    static final int SERVER_PORT = 10000;
    static  int seqno = 0;
    public static int uid=0;
    static int processseqno =-1;
    static int maxagreedno=-1;
    static  int max;
    int q =0;
    String fport="NOFAIL";
    PriorityQueue<Message> queue = new PriorityQueue<Message>(10, new MessageComparator());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        REMOTE_PORT.add("11108");
        REMOTE_PORT.add("11112");
        REMOTE_PORT.add("11116");
        REMOTE_PORT.add("11120");
        REMOTE_PORT.add("11124");
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        //System.out.println(tel.getLine1Number());
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        //System.out.println(portStr);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == android.view.KeyEvent.ACTION_DOWN) &&
                        (keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {
                    /*
                     * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
                     * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
                     * an AsyncTask that sends the string to the remote AVD.
                     */
                    String msg = editText.getText().toString() + "\n";
                    editText.setText(""); // This is one way to reset the input box.
                    TextView localTextView = (TextView) findViewById(R.id.textView1);
                    //localTextView.setMovementMethod(new ScrollingMovementMethod());
                    localTextView.append( msg); // This is one way to display a string.
                    localTextView.append("\n");

                    /*
                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                     * the difference, please take a look at
                     * http://developer.android.com/reference/android/os/AsyncTask.html
                     */
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    return true;
                }
                return false;
            }
        });

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        Button btn = (Button) findViewById(R.id.button4);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                //localTextView.setMovementMethod(new ScrollingMovementMethod());
                localTextView.append( msg); // This is one way to display a string.
                localTextView.append("\n");

                /*
                 * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                 * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                 * the difference, please take a look at
                 * http://developer.android.com/reference/android/os/AsyncTask.html
                 */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });


        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    ContentValues cv = new ContentValues();


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
/*
            try{
                while(true) {
                    Socket s = serverSocket.accept();
                    String str;

                    BufferedReader buf = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    while ((str = buf.readLine()) != null) {
                        publishProgress(str);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            */
            if(FALSE) {
                try {
                    while (true) {

                        Socket s = serverSocket.accept();
                        String str;

                        ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                        Message o = (Message) ois.readObject();

                        if (o.isMaxset == FALSE) {
                            if (processseqno <= maxagreedno) {
                                //processseqno = o.msgseqno ;
                                processseqno = maxagreedno + 1;


                            } else {
                                processseqno = processseqno + 1;
                            }
                            o.msgseqno = processseqno;
                            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                            oos.writeObject(o);
                            // Log.e(TAG,"Proposal Sent");
                        } else {
                            //Log.e(TAG,"GOT MAXIMUM AGREED Proposals ");
                            if (o.msgseqno > maxagreedno) {
                                maxagreedno = o.msgseqno;
                                queue.add(o);
                            } else {
                                //o.msgseqno = maxagreedno +1;
                                //maxagreedno++;
                                queue.add(o);
                            }

                            Message head = new Message();
                        /*if((head = queue.peek())!= null){
                            if(head.msgseqno<processseqno)
                                publishProgress(head.msg);
                                queue.remove(head);
                        }*/
                        /*
                        while(((head=queue.peek())!=null)&&(head.msgseqno<=processseqno)){
                            publishProgress((String)head.msg);
                            //out.println(head.msg + ":" + seqno);
                            Log.e(TAG,"Proposal  "+head.msg +":::"+ head.msgseqno);
                            queue.remove(head);
                        }
                        */

                            if (queue.size() == 20) {
                                while ((head = queue.poll()) != null) {
                                    publishProgress((head.msg));
                                }
                            }


                        }
                    }
                } catch (OptionalDataException e1) {
                    e1.printStackTrace();
                } catch (StreamCorruptedException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
            }

            try{
                while(true) {

                    Socket s = serverSocket.accept();
                    String str;

                    ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                    Message o = (Message) ois.readObject();

                    if(o.isMaxset == FALSE){
                        if(processseqno <= maxagreedno){
                            //processseqno = o.msgseqno ;
                            processseqno = maxagreedno + 1;

                        }
                        else{
                            processseqno = processseqno + 1;
                        }
                        o.msgseqno = processseqno;
                        queue.add(o);
                        ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                        oos.writeObject(o);
                        // Log.e(TAG,"Proposal Sent");
                    }
                    else{
                        //Log.e(TAG,"GOT MAXIMUM AGREED Proposals ");
                        if(o.msgseqno > maxagreedno) {
                            maxagreedno = o.msgseqno;
                            Iterator<Message> itr = queue.iterator();
                            while(itr.hasNext()){
                                Message h = (Message)itr.next();
                                if(h.msgid.equals(o.msgid)){
                                    itr.remove();
                                    break;

                                }
                            }
                            queue.add(o);
                        }
                        else{
                            //o.msgseqno = maxagreedno +1;
                            //maxagreedno++;
                            Iterator<Message> itr = queue.iterator();
                            while(itr.hasNext()){
                                Message h = (Message)itr.next();
                                if(h.msgid.equals(o.msgid)){
                                    itr.remove();
                                    break;

                                }
                            }
                            queue.add(o);
                        }

                        Message head = new Message();
                        /*if((head = queue.peek())!= null){
                            if(head.msgseqno<processseqno)
                                publishProgress(head.msg);
                                queue.remove(head);
                        }*/
                        /*
                        while(((head=queue.peek())!=null)&&(head.msgseqno<=processseqno)){
                            publishProgress((String)head.msg);
                            //out.println(head.msg + ":" + seqno);
                            Log.e(TAG,"Proposal  "+head.msg +":::"+ head.msgseqno);
                            queue.remove(head);
                        }
                        */
                        System.out.println(fport);
                            if(!fport.equals("NOFAIL") ){
                                head = queue.peek();
                                if(head.portno.equals(fport) && head.deliverable==FALSE){
                                    queue.poll();
                                    System.out.println("POLLLL!!!!!!!!");

                                }
                            }
                            while (!queue.isEmpty() && (head = queue.peek()).deliverable == TRUE) {
                                head = queue.poll();
                                publishProgress(head.msg);
                            }

                        /*
                        for(int i =0; i<queue.size();i++){
                            head = queue.peek();
                            if(head.deliverable==TRUE){
                                head = queue.poll();
                                publishProgress(head.msg);
                            }
                            else if(head.portno.equals(fport)) {
                                queue.poll();
                                Log.e(TAG, "POLL!!!!!!!!!!!");
                            }
                        }
                        */

                           // while(((head = queue.poll())!=null) && head.deliverable==TRUE){
                           //     publishProgress((head.msg));
                           // }



                    }
                }
            } catch (OptionalDataException e1) {
                e1.printStackTrace();
            } catch (StreamCorruptedException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
            /*
             * TODO: Fill in your server code that receives messages and passes them
             *
             * to onProgressUpdate().
             */
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            String strReceived = strings[0].trim();
            //System.out.println("RECEIVED STRING::::::::"+strReceived);
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\t\t\t" + strReceived + "\n");

            out.println("Content Provider :"+ strReceived + " : " +seqno );

            cv.put("key", Integer.toString(seqno));
            cv.put("value", strReceived);
            seqno = seqno + 1;


            getContentResolver().insert(mUri, cv);


            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override

        protected Void doInBackground(String... msgs) {
            ArrayList<Integer> proposedList = new ArrayList<Integer>();
            //processseqno = processseqno +1;
            String msgToSend = msgs[0];
            String ptr = msgs[1];
            String msgid = ptr +":" +uid;
            String portfail = null;
            Message mObject  = new Message(msgid,msgToSend,processseqno,FALSE,FALSE,ptr);
            for(String remotePort : REMOTE_PORT) {
                try {


                    portfail = remotePort;
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    socket.setSoTimeout(1000);

                    //String prt = msgs[1];
                    //System.out.println("MY PORT:::::::"+prt);
                    /*
                    q = q+1;
                    System.out.println("YOOO::::::::: "+q);
                    OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream());
                    PrintWriter out = new PrintWriter(osw);
                    out.write(msgToSend);
                    out.flush();
                    */

                    /*
                     * TODO: Fill in your client code that sends out a message.
                     */


                    //String msgid = ptr +":" +uid;
                    //Message mObject  = new Message(msgid,msgToSend,processseqno,FALSE,FALSE,ptr);
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(mObject);
                   // Log.e(TAG, "Initial Message Sent");
                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    Message recObj = (Message) ois.readObject();
                   // Log.e(TAG, "Got Proposals");
                    proposedList.add(recObj.msgseqno);


                    socket.close();


                }  catch (SocketTimeoutException e) {
                    //REMOTE_PORT.remove(REMOTE_PORT.indexOf(portfail));
                   //REMOTE_PORT.trimToSize();
                    fport=portfail;
                    Log.e(TAG, "Socket Timeout!!!!!!!!!!!!!");
                    e.printStackTrace();
                } catch(StreamCorruptedException e){
                    fport = portfail;
                }catch(FileNotFoundException e){
                    fport = portfail;
                }catch(EOFException e){
                    fport = portfail;
                }catch (IOException e) {

                    Log.e(TAG, "ClientTask socket IOException"+e.getMessage());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if(!fport.equals("NOFAIL") && REMOTE_PORT.contains(fport)){
                REMOTE_PORT.remove(REMOTE_PORT.indexOf(fport));
                REMOTE_PORT.trimToSize();
            }
            uid++;
            max = Collections.max(proposedList);
            mObject.setAgreedSeqno(max, TRUE,TRUE);
            for(String remotePort : REMOTE_PORT)   {
                try {
                    portfail = remotePort;
                    //Log.e(TAG,"PORTS:::::"+remotePort);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    socket.setSoTimeout(1000);
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(mObject);
                   // Log.e(TAG,"Maximum Agreed Sent ");
                    socket.close();
                } catch (SocketTimeoutException e) {
                    //REMOTE_PORT.remove(REMOTE_PORT.indexOf(portfail));
                    fport=portfail;
                    //REMOTE_PORT.trimToSize();
                    Log.e(TAG, "Socket Timeout!!!!!!!!!!!!!");
                    e.printStackTrace();
                } catch(StreamCorruptedException e){
                    Log.e(TAG, "Stream corr!!!!!!!!!!!!!");
                    fport = portfail;

                }catch(FileNotFoundException e){
                    Log.e(TAG, "FNF!!!!!!!!!!!!!");
                    fport = portfail;
                }catch(EOFException e){
                    Log.e(TAG, "EOF!!!!!!!!!!!!!");
                    fport = portfail;
                }
                catch (IOException e) {

                    Log.e(TAG,"Error Here Client Last Max"+e.getMessage());
                    e.printStackTrace();
                }

            }

            if(!fport.equals("NOFAIL") && REMOTE_PORT.contains(fport)){
                REMOTE_PORT.remove(REMOTE_PORT.indexOf(fport));
                REMOTE_PORT.trimToSize();
            }

            return null;
        }
    }
}

class Message implements Serializable {

    String msgid;
    String msg;
    int msgseqno;
    boolean isMaxset = FALSE;
    boolean deliverable = FALSE;
    String portno;

    public Message(){

    }

    public Message(String msgid, String msg, int msgseqno, boolean isMaxset, boolean deliverable, String portno) {
        this.msgid = msgid;
        this.msg = msg;
        this.msgseqno = msgseqno;
        this.isMaxset = isMaxset;
        this.deliverable = deliverable;
        this.portno = portno;
    }
    public void setAgreedSeqno(int seqno, boolean max,boolean deliverable){
        this.msgseqno = seqno;
        if (max) this.isMaxset = true;
        else this.isMaxset = false;
        this.deliverable = deliverable;
    }
}



class MessageComparator implements Comparator<Message> {


    public int compare(Message m1, Message m2) {
        if(m1.msgseqno < m2.msgseqno){
            return -1;
        }
        else if(m1.msgseqno> m2.msgseqno){
            return 1;
        }
        else{
            if(Integer.parseInt(m1.portno) < (Integer.parseInt(m2.portno))){
                return -1;
            }
            return 1;
        }
    }
}
