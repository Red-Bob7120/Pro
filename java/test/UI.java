// 파일명: UI.java
import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;

public class UI extends JFrame {

    // ======================= STYLE =======================
    private final Color COLOR_MINT = new Color(3, 199, 90);
    private final Color COLOR_BORDER = new Color(230, 230, 230);
    private final Font FONT_TITLE = new Font("맑은 고딕", Font.BOLD, 18);
    private final Font FONT_NORMAL = new Font("맑은 고딕", Font.PLAIN, 14);
    private final Font FONT_SMALL = new Font("맑은 고딕", Font.PLAIN, 12);

    // ======================= DATA ========================
    private final LinkedHashMap<String, Integer> cartMap = new LinkedHashMap<>();

    // ======================= UI ==========================
    private JTextField searchField;
    private JButton cartButton;
    private JPanel productListPanel;

    public UI() {
        super("마트 쇼핑앱 - 동선 최적화");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(430, 650);
        setLocationRelativeTo(null);

        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        initTopBar();
        initProductList();

        setVisible(true);
    }


    // =====================================================
    //  TOP BAR
    // =====================================================
    private void initTopBar() {

        JPanel top = panel(new BorderLayout(), 12);

        searchField = createTextField("상품 검색");

        JPanel searchBox = panel(new BorderLayout(), 6);
        searchBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        searchBox.add(new JLabel("🔍 "), BorderLayout.WEST);
        searchBox.add(searchField, BorderLayout.CENTER);

        cartButton = createCartButton();

        top.add(searchBox, BorderLayout.CENTER);
        top.add(cartButton, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
    }


    // =====================================================
    //  PRODUCT LIST
    // =====================================================
    private void initProductList() {

        String[] products = {
                "사과, 과일·야채",
                "바나나, 과일·야채",
                "우유, 신선식품",
                "특란 30구, 신선식품",
                "삼겹살, 정육",
                "닭가슴살, 정육",
                "왕교자, 냉동식품",
                "아이스크림, 간식",
                "샴푸, 생활용품",
                "세제, 생활용품"
        };

        productListPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        productListPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        productListPanel.setBackground(Color.WHITE);

        for (String p : products) {
            productListPanel.add(createProductCard(p));
        }

        JLabel title = new JLabel("추천 상품");
        title.setFont(FONT_TITLE);
        title.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel container = panel(new BorderLayout());
        container.add(title, BorderLayout.NORTH);
        container.add(scroll(productListPanel), BorderLayout.CENTER);

        add(container, BorderLayout.CENTER);
    }


    // =====================================================
    //  PRODUCT CARD
    // =====================================================
    private JPanel createProductCard(String data) {

        String[] p = data.split(",");
        String name = p[0].trim();
        String category = p[1].trim();

        JPanel card = cardPanel();

        JLabel nameLabel = label(name, FONT_NORMAL);
        JLabel categoryLabel = label(category, FONT_SMALL, Color.GRAY);

        JPanel textBox = panel(new GridLayout(2, 1));
        textBox.add(nameLabel);
        textBox.add(categoryLabel);

        JButton addBtn = button("담기", COLOR_MINT, Color.WHITE);
        addBtn.addActionListener(e -> {
            cartMap.put(name, cartMap.getOrDefault(name, 0) + 1);
            updateCartBadge();
        });

        card.add(textBox, BorderLayout.WEST);
        card.add(addBtn, BorderLayout.EAST);

        return card;
    }


    // =====================================================
    //  CART DIALOG
    // =====================================================
    private void openCartDialog() {

        JDialog dialog = new JDialog(this, "장바구니", true);
        dialog.setSize(360, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        dialog.add(label("🛒 장바구니", FONT_TITLE, null, 10), BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        for (String name : cartMap.keySet())
            listPanel.add(createCartRow(dialog, name, cartMap.get(name)));

        dialog.add(scroll(listPanel), BorderLayout.CENTER);

        JButton deleteBtn = button("선택 삭제", new Color(255, 80, 80), Color.WHITE);
        deleteBtn.addActionListener(e -> {
            Component[] rows = listPanel.getComponents();
            for (Component r : rows) {
                JPanel row = (JPanel) r;
                JCheckBox cb = (JCheckBox) row.getComponent(0);
                if (cb.isSelected())
                    cartMap.remove(cb.getText());
            }
            dialog.dispose();
            openCartDialog();
            updateCartBadge();
        });

        dialog.add(deleteBtn, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }


    // =====================================================
    //  CART ITEM ROW
    // =====================================================
    private JPanel createCartRow(JDialog dialog, String name, int qty) {

        JPanel row = cardPanel();
        row.setPreferredSize(new Dimension(320, 55));
        row.setMaximumSize(new Dimension(360, 55));

        JCheckBox cb = new JCheckBox(name);
        cb.setFont(FONT_NORMAL);
        cb.setBackground(Color.WHITE);

        JLabel qtyLabel = label(String.valueOf(qty), FONT_NORMAL);

        JButton minus = qtyBtn("－");
        JButton plus = qtyBtn("＋");

        minus.addActionListener(e -> {
            if (qty > 1)
                cartMap.put(name, qty - 1);
            else
                cartMap.remove(name);

            dialog.dispose();
            openCartDialog();
            updateCartBadge();
        });

        plus.addActionListener(e -> {
            cartMap.put(name, qty + 1);

            dialog.dispose();
            openCartDialog();
            updateCartBadge();
        });

        JPanel qtyPanel = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        qtyPanel.add(minus);
        qtyPanel.add(qtyLabel);
        qtyPanel.add(plus);

        row.add(cb, BorderLayout.WEST);
        row.add(qtyPanel, BorderLayout.EAST);

        return row;
    }


    // =====================================================
    //  UTIL METHODS
    // =====================================================
    private JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(Color.WHITE);
        return p;
    }

    private JPanel panel(LayoutManager layout, int padding) {
        JPanel p = panel(layout);
        p.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return p;
    }

    private JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(null);
        return s;
    }

    private JPanel cardPanel() {
        JPanel p = panel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return p;
    }

    private JLabel label(String text, Font f) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        return l;
    }

    private JLabel label(String text, Font f, Color color) {
        JLabel l = label(text, f);
        if (color != null) l.setForeground(color);
        return l;
    }

    private JLabel label(String text, Font f, Color color, int padding) {
        JLabel l = label(text, f, color);
        l.setBorder(BorderFactory.createEmptyBorder(padding, 12, padding, 12));
        return l;
    }

    private JButton button(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        return b;
    }

    private JButton createCartButton() {
        JButton b = button("🛒 0", Color.WHITE, Color.BLACK);
        b.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        return b;
    }

    private JTextField createTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.putClientProperty("JTextField.placeholderText", placeholder);
        tf.setFont(FONT_NORMAL);
        tf.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        return tf;
    }

    private JButton qtyBtn(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        b.setBackground(new Color(245, 245, 245));
        b.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
        b.setPreferredSize(new Dimension(40, 30));
        return b;
    }

    private void updateCartBadge() {
        cartButton.setText("🛒 " + cartMap.size());
    }


    // =============================== MAIN ===============================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(UI::new);
    }
}
