package com.example.measureppg;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ConfigActivity extends WearableActivity
{
    private TextView accText;
    private TextView hrText;

    private Button backButton;
    private Button accOn;
    private Button accOff;
    private Button hrOn;
    private Button hrOff;

    protected static boolean ACC;
    protected static boolean HR;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        accText = findViewById(R.id.accText);
        hrText = findViewById(R.id.hrText);

        backButton = findViewById(R.id.backButton);
        accOn = findViewById(R.id.accOnButton);
        accOff = findViewById(R.id.accOffButton);
        hrOn = findViewById(R.id.hrOnButton);
        hrOff = findViewById(R.id.hrOffButton);

        setAccOff();
        setHrOff();

        backButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        accOff.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setAccOff();
            }
        });

        accOn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setAccOn();
            }
        });

        hrOff.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setHrOff();
            }
        });

        hrOn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setHrOn();
            }
        });
    }

    private void setAccOff()
    {
        ACC = false;
        accText.setText("Accelerometer OFF");
    }

    private void setAccOn()
    {
        ACC = true;
        accText.setText("Accelerometer ON");
    }

    private void setHrOff()
    {
        HR = false;
        hrText.setText("HR Sensor OFF");
    }

    private void setHrOn()
    {
        HR = true;
        hrText.setText("HR Sensor ON");
    }
}
