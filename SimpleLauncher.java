import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class SimpleLauncher extends Application {
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0F1419;");
        
        Label titleLabel = new Label("英雄联盟 - 英雄选择器");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #C9AA71;");
        
        TextField searchField = new TextField();
        searchField.setPromptText("搜索英雄...");
        searchField.setPrefWidth(300);
        searchField.setStyle("-fx-background-color: #2c2c2c; -fx-text-fill: #CDBE91; -fx-border-color: #463714;");
        
        GridPane championGrid = new GridPane();
        championGrid.setHgap(10);
        championGrid.setVgap(10);
        championGrid.setAlignment(Pos.CENTER);
        
        // 创建示例英雄按钮
        String[] champions = {"金克丝", "亚索", "阿狸", "锤石", "李青", "盖伦", "拉克丝", "劫"};
        for (int i = 0; i < champions.length; i++) {
            Button championBtn = new Button(champions[i]);
            championBtn.setPrefSize(100, 100);
            championBtn.setStyle("-fx-background-color: #2c2c2c; -fx-text-fill: #CDBE91; -fx-border-color: #463714; -fx-border-width: 2px;");
            
            championBtn.setOnMouseEntered(e -> championBtn.setStyle("-fx-background-color: #C89B3C; -fx-text-fill: #000; -fx-border-color: #463714; -fx-border-width: 2px;"));
            championBtn.setOnMouseExited(e -> championBtn.setStyle("-fx-background-color: #2c2c2c; -fx-text-fill: #CDBE91; -fx-border-color: #463714; -fx-border-width: 2px;"));
            
            championGrid.add(championBtn, i % 4, i / 4);
        }
        
        VBox infoPanel = new VBox(10);
        infoPanel.setPadding(new Insets(15));
        infoPanel.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #463714; -fx-border-width: 2px;");
        
        Label selectedLabel = new Label("请选择一个英雄");
        selectedLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #C9AA71;");
        
        Label skillsLabel = new Label("技能信息将在这里显示");
        skillsLabel.setStyle("-fx-text-fill: #CDBE91;");
        skillsLabel.setWrapText(true);
        
        infoPanel.getChildren().addAll(selectedLabel, new Separator(), skillsLabel);
        
        HBox mainContent = new HBox(20);
        mainContent.getChildren().addAll(championGrid, infoPanel);
        
        root.getChildren().addAll(titleLabel, searchField, mainContent);
        
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("英雄选择器演示版");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("简化版英雄选择器已启动！");
        System.out.println("注意：这是一个演示版本，完整功能需要运行完整的Maven项目。");
    }
}