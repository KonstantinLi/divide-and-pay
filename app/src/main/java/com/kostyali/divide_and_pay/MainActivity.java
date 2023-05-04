package com.kostyali.divide_and_pay;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private final Context context = this;
    private final List<View> persons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton add = findViewById(R.id.add);
        ImageButton next = findViewById(R.id.next);
        final LinearLayout personView = findViewById(R.id.person_view);

        add.setOnClickListener(v -> {
            final View personItem = getLayoutInflater().inflate(R.layout.person_item_layout, null);
            final View prompt = getLayoutInflater().inflate(R.layout.set_name_layout, null);

            final EditText nameInput = prompt.findViewById(R.id.input_text);
            final EditText nameText = personItem.findViewById(R.id.person);
            ImageButton remove = personItem.findViewById(R.id.remove);

            remove.setOnClickListener(button -> {
                personView.removeView(personItem);
                persons.remove(personItem);
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(prompt);

            builder.setCancelable(false)
                    .setPositiveButton("OK", (dialog, id) -> {
                        String name = nameInput.getText().toString().trim();
                        if (name.isEmpty()) {
                            Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show();
                        } else if (persons.stream()
                                .map(view -> (EditText) view.findViewById(R.id.person))
                                .anyMatch(text -> {
                                    String nameCompare = text.getText().toString().trim();
                                    return nameCompare.compareToIgnoreCase(name) == 0;
                                })) {

                            Toast.makeText(this, "Имя занято", Toast.LENGTH_SHORT).show();

                        } else {
                            nameText.setText(name);
                            persons.add(personItem);
                            personView.addView(personItem);
                        }
                    })
                    .setNegativeButton("Отмена", (dialog, id) -> dialog.cancel());

            AlertDialog dialog = builder.create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        });

        next.setOnClickListener(v -> {
            if (persons.isEmpty()) {
                Toast.makeText(this, "Чтобы продолжить, добавьте хотя бы одного участника", Toast.LENGTH_SHORT).show();
            } else {
                String[] persons = this.persons.stream()
                        .map(view -> (EditText) view.findViewById(R.id.person))
                        .map(person -> person.getText().toString())
                        .toArray(String[]::new);

                Intent intent = new Intent(this, ProductActivity.class);
                intent.putExtra("persons", persons);
                startActivity(intent);
            }
        });
    }
}