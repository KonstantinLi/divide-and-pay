package com.kostyali.divide_and_pay;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.kostyali.divide_and_pay.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProductActivity extends AppCompatActivity {

    private static final List<Integer> COLORS = new ArrayList<Integer>() {{
        add(R.color.color1);
        add(R.color.color2);
        add(R.color.color3);
        add(R.color.color4);
        add(R.color.color5);
        add(R.color.color6);
    }};

    private final Map<Product, Set<String>> productsOfPersons = new HashMap<>();
    private final Set<Product> generalBasketItems = new HashSet<>();
    private final Set<Product> products = new HashSet<>();
    private final Context context = this;

    private String[] persons;
    private LinearLayout productView;
    private Button calculate;
    private Button basket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        persons = getIntent().getStringArrayExtra("persons");
        Button addProduct = findViewById(R.id.add_product);
        productView = findViewById(R.id.product_view);
        calculate = findViewById(R.id.calculate);
        basket = findViewById(R.id.basket);

        basket.setOnDragListener(onDragListener());
        basket.setOnClickListener(v -> {
            final View basketView = getLayoutInflater().inflate(R.layout.basket_layout, null);
            buildDialogBasket(basketView);
        });

        calculate.setOnClickListener(btn -> {
            List<Product> unusedProducts = products.stream()
                    .filter(product -> (!productsOfPersons.containsKey(product)
                            || productsOfPersons.get(product).isEmpty())
                            && !generalBasketItems.contains(product))
                    .collect(Collectors.toList());
            if (!unusedProducts.isEmpty()) {
                String text;
                if (unusedProducts.size() == 1)
                    text = "Продукт " + unusedProducts.get(0).getProduct() + " не внесён в общую корзину и не используется участниками группы";
                else {
                    StringJoiner joiner = new StringJoiner(", ");
                    unusedProducts.forEach(product -> joiner.add(product.getProduct()));
                    text = "Продукты " + joiner + " не внесены в общую корзину и не используются участниками группы";
                }
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            } else {
                final View resultLayout = getLayoutInflater().inflate(R.layout.result_layout, null);
                buildDialogResult(resultLayout);
            }
        });

        addProduct.setOnClickListener(v -> {
            final View productItem = getLayoutInflater().inflate(R.layout.product_item_layout, null);
            final View prompt = getLayoutInflater().inflate(R.layout.set_product_layout, null);

            setProductControls(productItem, prompt);
            buildDialogSave(prompt, productItem);
        });
    }

    private void setProductControls(View productItem, View prompt) {
        Button infoProduct = productItem.findViewById(R.id.product);
        ImageButton popupButton = productItem.findViewById(R.id.menu_persons);
        ImageButton removeProduct = productItem.findViewById(R.id.remove);

        infoProduct.setOnLongClickListener(onLongClickListener());

        removeProduct.setOnClickListener(button -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Удаление")
                    .setMessage("Вы уверены, что хотите удалить продукт?")
                    .setPositiveButton("Да", (dialog, id) -> {
                        removeProduct(findProduct(infoProduct.getText().toString()));
                        productView.removeView(productItem);
                    }).setNegativeButton("Нет", (dialog, id) -> dialog.cancel());
            builder.create().show();
        });

        infoProduct.setOnClickListener(button -> {
            Product product = findProduct(((Button) button).getText().toString());
            if (product != null) {
                buildDialogUpdate(prompt, productItem, product);
            }
        });

        popupButton.setOnClickListener(button -> {
            Product product = findProduct(infoProduct.getText().toString());
            showPopup(button, product);
        });
    }

    private void buildDialogUpdate(View prompt, View productItem, Product product) {
        removeView(prompt);

        EditText productInput = prompt.findViewById(R.id.product);
        EditText priceInput = prompt.findViewById(R.id.cost);
        EditText countInput = prompt.findViewById(R.id.count);

        productInput.setText(product.getProduct());
        priceInput.setText(String.valueOf(product.getPrice()));
        countInput.setText(String.valueOf(product.getCount()));

        Button infoProduct = productItem.findViewById(R.id.product);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(prompt);

        builder.setCancelable(false).setPositiveButton("Сохранить", (dialog, id) -> {
            try {
                String name = productInput.getText().toString().trim();
                double price = Double.parseDouble(priceInput.getText().toString());
                int count = Integer.parseInt(countInput.getText().toString());

                if (name.isEmpty()) {
                    Toast.makeText(this, "Введите название продукта", Toast.LENGTH_SHORT).show();
                } else {
                    updateProduct(product, name, price, count);
                    infoProduct.setText(name);
                }
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "Неверный формат данных", Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton("Отмена", (dialog, id) -> dialog.cancel());

        showDialog(builder);
    }

    private void buildDialogSave(View prompt, View productItem) {
        removeView(prompt);

        EditText productInput = prompt.findViewById(R.id.product);
        EditText priceInput = prompt.findViewById(R.id.cost);
        EditText countInput = prompt.findViewById(R.id.count);

        Button infoProduct = productItem.findViewById(R.id.product);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(prompt);

        builder.setCancelable(false).setPositiveButton("Добавить", (dialog, id) -> {
            try {
                String name = productInput.getText().toString().trim();
                double price = Double.parseDouble(priceInput.getText().toString());
                int count = Integer.parseInt(countInput.getText().toString());

                if (name.isEmpty()) {
                    Toast.makeText(this, "Введите название продукта", Toast.LENGTH_SHORT).show();
                } else if (findProduct(name) != null) {
                    Toast.makeText(this, "Такой продукт уже есть", Toast.LENGTH_SHORT).show();
                } else {
                    infoProduct.setText(name);
                    Product product = new Product(name, price, count);
                    products.add(product);
                    productsOfPersons.put(product, new HashSet<>());
                    productView.addView(productItem);
                }
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "Неверный формат данных", Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton("Отмена", (dialog, id) -> dialog.cancel());

        showDialog(builder);
    }

    private void buildDialogBasket(View basketView) {
        removeView(basketView);

        Context context = basketView.getContext();
        View tableView = ((ViewGroup) basketView).findViewById(R.id.table_layout);
        Drawable border = ContextCompat.getDrawable(context, R.drawable.border);

        for (Product productItem : generalBasketItems) {
            TableRow row = tableRow(context, border);

            List<TextView> productViews = productTextViews(productItem, context, border);
            ImageButton backupButton = backupImageButton(productItem, context, row, border);

            productViews.forEach(row::addView);
            row.addView(backupButton);

            ((ViewGroup) tableView).addView(row);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(basketView);

        builder.setCancelable(false).setNeutralButton("Ок", (dialog, id) -> {
            Set<Product> productsToAdd = products.stream()
                    .filter(product -> !(productsOfPersons.containsKey(product) || generalBasketItems.contains(product)))
                    .collect(Collectors.toSet());

            for (Product product : productsToAdd) {
                final View productItem = getLayoutInflater().inflate(R.layout.product_item_layout, null);
                final View prompt = getLayoutInflater().inflate(R.layout.set_product_layout, null);
                final Button infoProduct = productItem.findViewById(R.id.product);

                setProductControls(productItem, prompt);
                infoProduct.setText(product.getProduct());

                productView.addView(productItem);
                productsOfPersons.put(product, new HashSet<>());
                basket.setText("Общая корзина (" + generalBasketItems.size() + ")");
            }
        });

        showDialog(builder);
    }

    private void buildDialogResult(View resultView) {
        Context context = resultView.getContext();
        View tableView = ((ViewGroup) resultView).findViewById(R.id.table_layout);
        Drawable border = ContextCompat.getDrawable(context, R.drawable.border);

        for (String person : persons) {
            TableRow row = tableRow(context, border);

            TextView personText = commonTableTextView(context, border);
            TextView priceText = commonTableTextView(context, border);

            personText.setText(person);
            priceText.setText(String.valueOf(priceForPerson(person)));

            row.addView(personText);
            row.addView(priceText);
            ((ViewGroup) tableView).addView(row);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(resultView);

        builder.setCancelable(false).setPositiveButton("Новая сессия", (dialog, id) -> {
            Intent intent = new Intent(this.context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }).setNegativeButton("Вернуться", (dialog, id) -> dialog.cancel());

        showDialog(builder);
    }

    private double priceForPerson(String person) {
        double generalPrice = generalBasketItems.stream().mapToDouble(product -> product.getPrice() * product.getCount()).sum() / persons.length;
        return generalPrice + productsOfPersons.keySet()
                .stream()
                .filter(product -> productsOfPersons.get(product).contains(person))
                .mapToDouble(product -> product.getPrice() * product.getCount() / productsOfPersons.get(product).size())
                .sum();
    }

    private List<TextView> productTextViews(Product product, Context context, Drawable background) {
        List<TextView> textViews = IntStream.range(0, 3)
                .mapToObj((i) -> commonTableTextView(context, background))
                .collect(Collectors.toList());

        for (int i = 0; i < 3; i++) {
            TextView textView = textViews.get(i);
            switch (i) {
                case 0:
                    textView.setText(product.getProduct());
                    break;
                case 1:
                    textView.setText(String.valueOf(product.getPrice()));
                    break;
                default:
                    textView.setText(String.valueOf(product.getCount()));
            }

            if (textView.getText().toString().length() >= 10) {
                TableRow.LayoutParams lp = (TableRow.LayoutParams) textView.getLayoutParams();
                lp.gravity = Gravity.FILL;
                lp.width = 1;
            }
        }

        return textViews;
    }

    private TextView commonTableTextView(Context context, Drawable background) {
        TableRow.LayoutParams lp = new TableRow.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        TextView textView = new TextView(context);
        textView.setLayoutParams(lp);
        textView.setBackground(background);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        textView.setTextSize(18);
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setMaxLines(1);

        return textView;
    }

    private ImageButton backupImageButton(Product product, Context context, View view, Drawable background) {
        TableRow.LayoutParams lp = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(5, 5, 5, 5);
        lp.height = 70;

        ImageButton button = new ImageButton(context);
        button.setLayoutParams(lp);
        button.setBackground(background);
        button.setBackgroundColor(getResources().getColor(R.color.color5, getTheme()));
        button.setImageResource(R.drawable.baseline_settings_backup_restore_24);

        button.setOnClickListener(btn -> {
            ((ViewGroup) view.getParent()).removeView(view);
            generalBasketItems.remove(product);
        });

        return button;
    }

    private TableRow tableRow(Context context, Drawable background) {
        TableRow.LayoutParams lp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT);

        TableRow row = new TableRow(context);
        row.setLayoutParams(lp);
        row.setBackground(background);
        row.setGravity(Gravity.CENTER_VERTICAL);

        return row;
    }

    public void showPopup(View view, Product product) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.inflate(R.menu.overflow_menu);
        Menu menu = popup.getMenu();

        for (String person : persons) {
            MenuItem item = menu.add(R.id.persons, Menu.NONE, Menu.NONE, person);
            item.setCheckable(true);
            if (productsOfPersons.containsKey(product) && productsOfPersons.get(product).contains(person)) {
                item.setChecked(true);
            }
        }

        final LinearLayout personProductView = ((ViewGroup) view.getParent()).findViewById(R.id.person_layout);

        popup.setOnMenuItemClickListener(itemClickListener(product));
        popup.setOnDismissListener(dismissListener(personProductView, product));

        popup.show();
    }

    private PopupMenu.OnDismissListener dismissListener(LinearLayout personProductView, Product product) {
        return (menu) -> {
            personProductView.removeAllViews();
            Set<String> persons = productsOfPersons.get(product);

            if (persons != null) {
                int order = 0;
                int count = COLORS.size();
                for (String person : persons) {
                    final View personItem = getLayoutInflater().inflate(R.layout.person_product_layout, null);
                    final TextView personTextView = personItem.findViewById(R.id.person_product);

                    GradientDrawable background = (GradientDrawable) personTextView.getBackground();
                    background.setColor(ContextCompat.getColor(
                            this,
                            COLORS.get(++order <= count ? order - 1 : order % count)));

                    personTextView.setText(person);
                    personProductView.addView(personItem);
                }
            }
        };
    }

    private PopupMenu.OnMenuItemClickListener itemClickListener(Product product) {
        return (item) -> {
            String person = item.getTitle().toString();
            if (!item.isChecked()) {
                if (productsOfPersons.containsKey(product)) {
                    Objects.requireNonNull(productsOfPersons.get(product)).add(person);
                } else {
                    Set<String> set = new HashSet<>();
                    set.add(person);
                    productsOfPersons.put(product, set);
                }
                item.setChecked(true);
            } else {
                Objects.requireNonNull(productsOfPersons.get(product)).remove(person);
                item.setChecked(false);
            }
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            item.setActionView(new View(context));
            item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item11) {
                    return false;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item11) {
                    return false;
                }
            });
            return false;
        };
    }

    private void showDialog(AlertDialog.Builder builder) {
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private Product findProduct(String product) {
        return products.stream()
                .filter(item -> item.getProduct().equals(product))
                .findAny()
                .orElse(null);
    }

    private void removeView(View view) {
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
    }

    private void updateProduct(Product product, String name, double price, int count) {
        product.setProduct(name);
        product.setPrice(price);
        product.setCount(count);
    }

    private void removeProduct(Product product) {
        products.remove(product);
        productsOfPersons.remove(product);
    }

    private View.OnLongClickListener onLongClickListener() {
        return v -> {
            String product = ((Button) v).getText().toString();

            ClipData.Item item = new ClipData.Item(product);
            ClipData dataToDrag = new ClipData("copy", new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);

            DragShadowBuilder builder = new DragShadowBuilder(v);
            v.startDragAndDrop(dataToDrag, builder, v, 0);

            ((View) v.getParent()).setVisibility(View.GONE);
            return true;
        };
    }

    private View.OnDragListener onDragListener() {
        return (v, dragEvent) -> {
            View draggableItem = (View) ((View) dragEvent.getLocalState()).getParent();
            switch (dragEvent.getAction()) {
                case (DragEvent.ACTION_DRAG_STARTED):
                case (DragEvent.ACTION_DRAG_LOCATION):
                    return true;
                case (DragEvent.ACTION_DRAG_ENDED): {
                    if (dragEvent.getResult()) {
                        return true;
                    }
                    draggableItem.setVisibility(View.VISIBLE);
                    v.invalidate();
                    return false;
                }
                case (DragEvent.ACTION_DRAG_ENTERED): {
                    basket.setAlpha(0.3f);
                    v.invalidate();
                    return true;
                }
                case (DragEvent.ACTION_DRAG_EXITED): {
                    basket.setAlpha(1.0f);
                    return true;
                }
                case (DragEvent.ACTION_DROP): {
                    basket.setAlpha(1.0f);
                    if (dragEvent.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        String draggedData = dragEvent.getClipData().getItemAt(0).getText().toString();
                        Product product = findProduct(draggedData);
                        if (product != null) {
                            productsOfPersons.remove(product);
                            generalBasketItems.add(product);
                            basket.setText("Общая корзина (" + generalBasketItems.size() + ")");
                        }
                    }
                    return true;
                }
                default: return false;
            }
        };
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Перезапуск")
                .setMessage("Хотите вернуться на начальную страницу? Все данные будут стёрты")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Да", (arg0, arg1) -> {
                    Intent intent = new Intent(this.context, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }).create().show();
    }

    private static class DragShadowBuilder extends View.DragShadowBuilder {
        private final View view;

        public DragShadowBuilder(View view) {
            super(view);
            this.view = view;
        }

        @Override
        public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
            int width = view.getWidth();
            int height = view.getHeight();
            outShadowSize.set(width, height);
            outShadowTouchPoint.set(width, height);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            view.draw(canvas);
        }
    }
}