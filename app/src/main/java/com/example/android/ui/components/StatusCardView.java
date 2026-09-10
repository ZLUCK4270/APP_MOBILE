package com.example.android.ui.components;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.android.R;
import com.google.android.material.card.MaterialCardView;

public class StatusCardView extends MaterialCardView {

    private ImageView cardIcon;
    private TextView cardTitle;
    private TextView cardValue;
    private TextView cardUnit;

    public StatusCardView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public StatusCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public StatusCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_status_card, this, true);

        cardIcon = findViewById(R.id.cardIcon);
        cardTitle = findViewById(R.id.cardTitle);
        cardValue = findViewById(R.id.cardValue);
        cardUnit = findViewById(R.id.cardUnit);

        setRadius(getResources().getDisplayMetrics().density * 20); // 20dp
        setStrokeWidth(0);
        setCardElevation(getResources().getDisplayMetrics().density * 2); // 2dp

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StatusCardView);

            String title = a.getString(R.styleable.StatusCardView_cardTitle);
            String unit = a.getString(R.styleable.StatusCardView_cardUnit);
            int iconRes = a.getResourceId(R.styleable.StatusCardView_cardIcon, 0);
            ColorStateList tintColor = a.getColorStateList(R.styleable.StatusCardView_cardTintColor);

            if (title != null) cardTitle.setText(title);
            if (unit != null) cardUnit.setText(unit);
            if (iconRes != 0) cardIcon.setImageResource(iconRes);
            
            if (tintColor != null) {
                cardTitle.setTextColor(tintColor);
                cardValue.setTextColor(tintColor);
                cardUnit.setTextColor(tintColor);
                cardIcon.setImageTintList(tintColor);
            }

            a.recycle();
        }
    }

    public void setValue(String value) {
        cardValue.setText(value);
    }
}
