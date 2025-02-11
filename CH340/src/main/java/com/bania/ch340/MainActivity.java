package com.bania.ch340;

import androidx.appcompat.app.AppCompatActivity;
import cn.wch.ch34xuartdriver.CH34xUARTDriver;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    private final int baudRate = 9600;
    private final byte dataBit = 8;
    private final byte parity = 0;
    private final byte stopBit = 1;
    private final byte flowControl = 0;

    private boolean isOpen;

    private Handler handler;
    private MainActivity activity;

    private TextView readText;
    private Button openButton;

    public byte[] writeBuffer;
    public byte[] readBuffer;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        readText = (TextView) findViewById(R.id.ReadText);
        openButton = (Button) findViewById(R.id.openButton);

        Driver.CH340 = new CH34xUARTDriver( // initiate driver CH340
                        (UsbManager) getSystemService(Context.USB_SERVICE), this,
                        ACTION_USB_PERMISSION);

        if (!Driver.CH340.UsbFeatureSupported()) // check support to USB HOST
        {
            Dialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("ERROR")
                    .setMessage("DON'T SUPPORT USB HOST！")
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener()
                            {

                                @Override
                                public void onClick(DialogInterface arg0, int arg1)
                                {
                                    System.exit(0);
                                }
                            }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // keep screen on

        writeBuffer = new byte[512]; // create buffor to write
        readBuffer = new byte[512]; // create buffor to read
        isOpen = false; // port in CH340 is close

        activity = this;

        openButton.setOnClickListener(new View.OnClickListener() // click open button
        {

            @Override
            public void onClick(View arg0) // if click
            {
                if (!isOpen) // if port in CH340 is not opened
                {
                    int retval = Driver.CH340.ResumeUsbPermission(); // permission to check USB

                    if (retval == 0) // if don't find device USB
                    {
                        retval = Driver.CH340.ResumeUsbList(); // resume check device USB

                        if (retval == -1) // if device can't opened
                        {
                            Toast.makeText(MainActivity.this, "Open failed!",
                            Toast.LENGTH_SHORT).show();

                            Driver.CH340.CloseDevice(); // turn off device
                        }
                        else if (retval == 0)
                        {
                            if (Driver.CH340.mDeviceConnection != null)
                            {
                                if (!Driver.CH340.UartInit()) // if device can't initiate
                                {
                                    Toast.makeText(MainActivity.this, "Initialization failed!",
                                    Toast.LENGTH_SHORT).show();

                                    return;
                                }

                                // if device initiated and opened
                                Toast.makeText(MainActivity.this, "Device opened",
                                Toast.LENGTH_SHORT).show();

                                Driver.CH340.SetConfig(baudRate, dataBit, stopBit, parity, flowControl); // set config

                                openButton.setText("Close"); // change text in openButton to close
                                isOpen = true; // port in CH340 is open

                                new readThread().start(); // start readThread to recive data of CH340
                            }
                            else
                            {
                                // if device can't initiated and opened
                                Toast.makeText(MainActivity.this, "Open failed!",
                                Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            builder.setTitle("ERROR");
                            builder.setMessage("TIMEOUT!");
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                            {

                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    System.exit(0);
                                }
                            });
                            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener()
                            {

                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    // TODO Auto-generated method stub

                                }
                            });
                            builder.show();
                        }
                    }
                }
                else
                {
                    openButton.setText("Open"); // change text in openButton to close
                    isOpen = false; // port in CH340 is close

                    try
                    {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    Driver.CH340.CloseDevice(); // turn off device
                }
            }
        });

        handler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                readText.append((String) msg.obj); // add msg to readText
            }
        };

    }

    @Override
    public void onResume()
    {
        super.onResume();
    }


    @Override
    public void onDestroy()
    {
        isOpen = false; // port in CH340 is close
        Driver.CH340.CloseDevice(); // turn off device
        super.onDestroy(); // turn off application
    }

    private class readThread extends Thread // readThread function to receive data from serial
    {
        public void run()
        {
            byte[] buffer = new byte[4096];

            while (true)
            {
                Message msg = Message.obtain();

                if (!isOpen) // if port in CH340 is close, break
                {
                    break;
                }

                int length = Driver.CH340.ReadData(buffer, 4096); // length receive data

                if (length > 0) // if serial have receive data
                {
					String recv = new String(buffer, 0, length); // receive data to String
                    msg.obj = recv; // receive data to msg object
                    handler.sendMessage(msg); // msg object to handler (send to readText)
                }
            }
        }
    }
}