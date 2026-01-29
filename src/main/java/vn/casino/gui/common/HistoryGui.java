package vn.casino.gui.common;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.casino.core.scheduler.FoliaScheduler;
import vn.casino.economy.Transaction;
import vn.casino.economy.TransactionRepository;
import vn.casino.economy.TransactionType;
import vn.casino.gui.framework.GuiButton;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageManager;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Paginated transaction history GUI.
 * Shows last 45 transactions with navigation.
 */
public class HistoryGui extends ChestGui {

    private static final int PAGE_SIZE = 45; // 9x5 items per page
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    private final Player player;
    private final TransactionRepository transactionRepository;
    private final GuiManager guiManager;
    private final FoliaScheduler scheduler;

    public HistoryGui(
        Player player,
        TransactionRepository transactionRepository,
        GuiManager guiManager,
        FoliaScheduler scheduler,
        MessageManager messageManager
    ) {
        super(6, "Lịch sử giao dịch");
        this.player = player;
        this.transactionRepository = transactionRepository;
        this.guiManager = guiManager;
        this.scheduler = scheduler;

        setupLayout();
    }

    private void setupLayout() {
        // Load transactions asynchronously
        transactionRepository.findByPlayer(player.getUniqueId(), PAGE_SIZE)
            .thenAccept(transactions -> {
                scheduler.runAtEntity(player, () -> {
                    PaginatedPane pages = new PaginatedPane(0, 0, 9, 5);

                    // Convert transactions to GUI items
                    transactions.forEach(tx -> {
                        pages.addPane(0, createTransactionItem(tx));
                    });

                    // Navigation bar
                    StaticPane navBar = new StaticPane(0, 5, 9, 1);

                    // Previous page button
                    navBar.addItem(GuiButton.create(
                        Material.ARROW,
                        "<yellow>← Trang trước</yellow>",
                        List.of("<gray>Click để xem trang trước</gray>"),
                        e -> {
                            if (pages.getPage() > 0) {
                                pages.setPage(pages.getPage() - 1);
                                update();
                            }
                        }
                    ).toGuiItem(), 0, 0);

                    // Page indicator
                    navBar.addItem(GuiButton.createDisplay(
                        Material.BOOK,
                        "<white>Trang " + (pages.getPage() + 1) + "/" + Math.max(1, pages.getPages()) + "</white>",
                        List.of("<gray>" + transactions.size() + " giao dịch</gray>")
                    ).toGuiItem(), 4, 0);

                    // Next page button
                    navBar.addItem(GuiButton.create(
                        Material.ARROW,
                        "<yellow>Trang sau →</yellow>",
                        List.of("<gray>Click để xem trang sau</gray>"),
                        e -> {
                            if (pages.getPage() < pages.getPages() - 1) {
                                pages.setPage(pages.getPage() + 1);
                                update();
                            }
                        }
                    ).toGuiItem(), 8, 0);

                    // Close button
                    navBar.addItem(GuiButton.create(
                        Material.BARRIER,
                        "<red>Đóng</red>",
                        List.of("<gray>Đóng lịch sử</gray>"),
                        e -> player.closeInventory()
                    ).toGuiItem(), 4, 0);

                    addPane(pages);
                    addPane(navBar);
                    update();
                });
            })
            .exceptionally(ex -> {
                player.sendMessage("§cKhông thể tải lịch sử giao dịch!");
                player.closeInventory();
                return null;
            });
    }

    /**
     * Create GUI item for a transaction.
     */
    private StaticPane createTransactionItem(Transaction tx) {
        StaticPane pane = new StaticPane(0, 0, 1, 1);

        Material material = switch (tx.type()) {
            case WIN, JACKPOT -> Material.EMERALD;
            case BET -> Material.REDSTONE;
            case GIVE, ADMIN_GIVE -> Material.DIAMOND;
            case TAKE, ADMIN_TAKE -> Material.COAL;
            case DEPOSIT -> Material.GOLD_INGOT;
            case WITHDRAW -> Material.IRON_INGOT;
            case REFUND -> Material.YELLOW_STAINED_GLASS;
        };

        String color = (tx.type() == TransactionType.WIN || tx.type() == TransactionType.GIVE)
            ? "<green>" : "<red>";
        String sign = (tx.type() == TransactionType.WIN || tx.type() == TransactionType.GIVE)
            ? "+" : "-";

        String formattedAmount = CURRENCY_FORMAT.format(tx.amount());
        String formattedDate = DATE_FORMAT.format(
            tx.createdAt().atZone(ZoneId.systemDefault()).toLocalDateTime()
        );

        pane.addItem(GuiButton.createDisplay(
            material,
            color + sign + formattedAmount + " VND</color>",
            List.of(
                "<gray>Loại: " + getTypeName(tx.type()) + "</gray>",
                "<gray>Game: " + tx.game() + "</gray>",
                "<gray>Thời gian: " + formattedDate + "</gray>",
                "",
                "<dark_gray>" + tx.description() + "</dark_gray>"
            )
        ).toGuiItem(), 0, 0);

        return pane;
    }

    /**
     * Get Vietnamese name for transaction type.
     */
    private String getTypeName(TransactionType type) {
        return switch (type) {
            case BET -> "Cược";
            case WIN -> "Thắng";
            case JACKPOT -> "Jackpot";
            case GIVE, ADMIN_GIVE -> "Nhận";
            case TAKE, ADMIN_TAKE -> "Mất";
            case DEPOSIT -> "Nạp";
            case WITHDRAW -> "Rút";
            case REFUND -> "Hoàn";
        };
    }
}
