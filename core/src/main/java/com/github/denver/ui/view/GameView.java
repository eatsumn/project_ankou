package com.github.denver.ui.view;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.TextraLabel;
import com.github.tommyettinger.textra.TypingLabel;
import com.github.denver.system.GameSessionSystem;
import com.github.denver.ui.model.GameViewModel;

import java.util.Map;

public class GameView extends View<GameViewModel> {
    private final HorizontalGroup lifeGroup;
    private final Label timerLabel;
    private final Label ghostKillsLabel;
    private final Table endOverlay;
    private final Label endResultLabel;

    public GameView(Stage stage, Skin skin, GameViewModel viewModel) {
        super(stage, skin, viewModel);

        this.lifeGroup = findActor("lifeGroup");
        this.timerLabel = findActor("timerLabel");
        this.ghostKillsLabel = findActor("ghostKillsLabel");
        this.endOverlay = findActor("endOverlay");
        this.endResultLabel = findActor("endResultLabel");

        updateLife(viewModel.getLifePoints());
        updateGhostKills(viewModel.getGhostKills());
        timerLabel.setText(formatTime(viewModel.getSessionTime()));
        endOverlay.setVisible(viewModel.isSessionComplete());
        if (viewModel.isSessionComplete()) {
            refreshEndSummary();
        }
    }

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);
        if (stage != null && endOverlay != null) {
            endOverlay.toFront();
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        timerLabel.setText(formatTime(viewModel.getSessionTime()));
    }

    private static String formatTime(float secondsRaw) {
        float seconds = MathUtils.clamp(secondsRaw, 0f, GameSessionSystem.SESSION_DURATION_SEC);
        int total = (int) seconds;
        int m = total / 60;
        int s = total % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    @Override
    protected void setupPropertyChanges() {
        viewModel.onPropertyChange(GameViewModel.LIFE_POINTS, Integer.class, this::updateLife);
        viewModel.onPropertyChange(GameViewModel.PLAYER_DAMAGE, Map.Entry.class, this::showDamage);
        viewModel.onPropertyChange(GameViewModel.GHOST_KILLS, Integer.class, this::updateGhostKills);
        viewModel.onPropertyChange(GameViewModel.SESSION_COMPLETE, Boolean.class, this::onSessionCompleteChanged);
    }

    private void onSessionCompleteChanged(Boolean complete) {
        boolean show = Boolean.TRUE.equals(complete);
        endOverlay.setVisible(show);
        endOverlay.setTouchable(show ? Touchable.enabled : Touchable.disabled);
        if (show) {
            refreshEndSummary();
            endOverlay.toFront();
        }
    }

    private void refreshEndSummary() {
        endResultLabel.setText("Ghosts eliminated: " + viewModel.getGhostKills());
    }

    private void updateGhostKills(int kills) {
        ghostKillsLabel.setText("Ghosts: " + kills);
    }

    @Override
    protected void setupUI() {
        setFillParent(true);

        Table topBar = new Table();
        topBar.setFillParent(true);
        topBar.top().right().pad(6f);
        Label timer = new Label("0:00", skin, "small");
        timer.setName("timerLabel");
        timer.setAlignment(Align.right);
        timer.setColor(skin.getColor("black"));
        topBar.add(timer);
        addActor(topBar);

        Table bottomBar = new Table();
        bottomBar.setFillParent(true);
        bottomBar.bottom().pad(5f);
        bottomBar.defaults().expandX();

        HorizontalGroup horizontalGroup = new HorizontalGroup();
        horizontalGroup.setName("lifeGroup");
        horizontalGroup.padLeft(5f);
        horizontalGroup.space(5.0f);
        bottomBar.add(horizontalGroup).left();

        Label ghosts = new Label("Ghosts: 0", skin, "small");
        ghosts.setName("ghostKillsLabel");
        ghosts.setAlignment(Align.right);
        ghosts.setColor(skin.getColor("black"));
        bottomBar.add(ghosts).right();
        addActor(bottomBar);

        Table overlay = new Table();
        overlay.setName("endOverlay");
        overlay.setFillParent(true);
        overlay.setVisible(false);
        overlay.setTouchable(Touchable.disabled);
        overlay.setBackground(skin.getDrawable("frame"));
        overlay.pad(16f);

        Label title = new Label("Yey you've done it!!", skin);
        title.setColor(skin.getColor("sand"));
        overlay.add(title).row();

        Label result = new Label("", skin, "small");
        result.setName("endResultLabel");
        result.setColor(skin.getColor("white"));
        overlay.add(result).padTop(8f).row();

        TextButton menu = new TextButton("Menu", skin);
        View.onClick(menu, viewModel::returnToMenu);
        overlay.add(menu).padTop(14f);

        addActor(overlay);
    }

    /**
     * Updates the life display with appropriate heart icons.
     */
    private void updateLife(int lifePoints) {
        lifeGroup.clear();

        int maxLife = viewModel.getMaxLife();
        while (maxLife > 0) {
            int imgIdx = MathUtils.clamp(lifePoints, 0, 4);
            Image image = new Image(skin, "life_0" + imgIdx);
            lifeGroup.addActor(image);

            maxLife -= 4;
            lifePoints -= 4;
        }
    }

    private Vector2 toStageCoords(Vector2 gamePosition) {
        Vector2 resultPosition = viewModel.toScreenCoords(gamePosition);
        stage.getViewport().unproject(resultPosition);
        resultPosition.y = stage.getViewport().getWorldHeight() - resultPosition.y;
        return resultPosition;
    }

    /**
     * Shows animated damage text at the specified position.
     */
    private void showDamage(Map.Entry<Vector2, Integer> damAndPos) {
        final Vector2 position = damAndPos.getKey();
        int damage = damAndPos.getValue();

        TextraLabel textraLabel = new TypingLabel("[%75]{JUMP=2.0;0.5;0.9}{RAINBOW}" + damage, skin, "small");
        stage.addActor(textraLabel);

        textraLabel.addAction(
            Actions.parallel(
                Actions.sequence(Actions.delay(1.25f), Actions.removeActor()),
                Actions.forever(Actions.run(() -> {
                    Vector2 stageCoords = toStageCoords(position);
                    textraLabel.setPosition(stageCoords.x, stageCoords.y);
                }))
            )
        );
    }
}
