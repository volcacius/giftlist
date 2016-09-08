package it.polimi.dima.giftlist.presentation.view.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.appyvet.rangebar.RangeBar;
import com.hannesdorfmann.fragmentargs.annotation.Arg;
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs;
import com.redbooth.WelcomeCoordinatorLayout;
import com.robinhood.ticker.TickerUtils;
import com.robinhood.ticker.TickerView;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.Bind;
import butterknife.OnClick;
import icepick.State;
import it.polimi.dima.giftlist.R;
import it.polimi.dima.giftlist.data.model.CategoryType;
import it.polimi.dima.giftlist.data.repository.datasource.EbayProductDataSource;
import it.polimi.dima.giftlist.data.repository.datasource.EtsyProductDataSource;
import it.polimi.dima.giftlist.presentation.component.ProductPickerSettingsComponent;
import it.polimi.dima.giftlist.presentation.navigation.IntentStarter;
import it.polimi.dima.giftlist.presentation.presenter.ProductPickerSettingsPresenter;
import it.polimi.dima.giftlist.presentation.view.ProductPickerSettingsView;
import mabbas007.tagsedittext.TagsEditText;
import timber.log.Timber;

/**
 * Created by Elena on 10/02/2016.
 */
@FragmentWithArgs
public class ProductPickerSettingsFragment extends BaseMvpFragment<ProductPickerSettingsView, ProductPickerSettingsPresenter> {

    private static final String EMPTY_STRING = "";
    private static final Float DEFAULT_MAX = (float) 1000.0;
    private static final Float DEFAULT_MIN = (float) 0.0;
    private static final int MAX_PAGE = 3;

    @Arg
    @State
    long wishlistId;

    @State
    int startingDisplayOrder;

    WelcomeCoordinatorLayout coordinatorLayout;

    @Bind(R.id.select_age)
    MaterialBetterSpinner ageSpinner;
    @Bind(R.id.search_tags)
    TagsEditText tagsEditText;

    @Bind(R.id.checkbox_art)
    CheckBox artCheckbox;
    @Bind(R.id.checkbox_games)
    CheckBox gamesCheckbox;
    @Bind(R.id.checkbox_sports)
    CheckBox sportsCheckbox;
    @Bind(R.id.checkbox_technology)
    CheckBox techCheckbox;
    @Bind(R.id.checkbox_travelling)
    CheckBox travelCheckbox;
    @Bind(R.id.checkbox_handcraft)
    CheckBox handcraftCheckbox;
    @Bind(R.id.checkbox_nerd)
    CheckBox nerdCheckbox;
    @Bind(R.id.checkbox_books)
    CheckBox bookCheckbox;
    @Bind(R.id.checkbox_music)
    CheckBox musicCheckbox;

    @Bind(R.id.checkbox_ebay)
    CheckBox ebayCheckbox;
    @Bind(R.id.checkbox_etsy)
    CheckBox etsyCheckbox;

    @Bind(R.id.rangebar)
    RangeBar rangeBar;
    @Bind(R.id.ticker_min)
    TickerView tickerMinView;
    @Bind(R.id.ticker_max)
    TickerView tickerMaxView;

    @Bind(R.id.skip)
    Button skipButton;
    @Bind(R.id.next)
    Button nextButton;
    @Bind(R.id.finish)
    Button finishButton;

    private boolean animationReady = false;
    private ValueAnimator backgroundAnimator;

    Snackbar snackBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //Because of the order of initializations, I can't use Butterknife for coordinaorLayout
        //First: coordinator binding
        //Second: page init
        //Third: butterknife binding
        coordinatorLayout = (WelcomeCoordinatorLayout) view.findViewById(R.id.coordinator);
        initPages();
        super.onViewCreated(view, savedInstanceState);
        initListeners();
        initBackgroundTransitions();
        tagsEditText.setText(EMPTY_STRING);

        snackBar = Snackbar.make(
                view,
                R.string.pick_source_error,
                Snackbar.LENGTH_SHORT);

        snackBar.setAction(R.string.snack_bar_action_dismiss, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackBar.dismiss();
            }
        });
        snackBar.setActionTextColor(ContextCompat.getColor(getContext(), R.color.snackbar_action_color_done));

        ebayCheckbox.setChecked(true);
        etsyCheckbox.setChecked(true);

        startingDisplayOrder = presenter.getStartingProductDisplayOrder(wishlistId);

        tickerMinView.setCharacterList(TickerUtils.getDefaultListForUSCurrency());
        tickerMaxView.setCharacterList(TickerUtils.getDefaultListForUSCurrency());
        rangeBar.setRangePinsByValue(DEFAULT_MIN, DEFAULT_MAX);
        tickerMinView.setText(String.format("%.1f$", DEFAULT_MIN));
        tickerMaxView.setText(String.format("%.1f$", DEFAULT_MAX));
        rangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
                                              int rightPinIndex,
                                              String leftPinValue, String rightPinValue) {
                tickerMinView.setText(String.format("%s.0$", leftPinValue));
                tickerMaxView.setText(String.format("%s.0$", rightPinValue));
            }
        });
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.spinner_dropdown_color,
                getContext().getResources().getStringArray(R.array.ages));
        ageSpinner.setAdapter(adapter);
        ageSpinner.setTextColor(Color.WHITE);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_product_picker_settings;
    }

    @Override
    public ProductPickerSettingsPresenter createPresenter() {
        return this.getComponent(ProductPickerSettingsComponent.class).providePresenter();
    }

    @Override
    protected void injectDependencies() {
        this.getComponent(ProductPickerSettingsComponent.class).inject(this);
    }

    public void startProductActivity() {
        HashMap<Class,Boolean> enabledRepositoryMap = getEnabledRepositoriesMap();
        if (!enabledRepositoryMap.get(EbayProductDataSource.class)
                && !enabledRepositoryMap.get(EtsyProductDataSource.class)) {
            snackBar.show();
            return;
        }
        ArrayList<CategoryType> chosenCategoriesList = getChosenCategoriesFromUI(String.valueOf(ageSpinner.getText()));
        Timber.d("Age selected is %s", String.valueOf(ageSpinner.getText()));
        Timber.d("Tags are: %s", tagsEditText.getText().toString());
        Float minprice;
        Float maxprice;
        try {
            minprice =  Float.parseFloat(tickerMinView.toString());
        } catch (NumberFormatException e) {
            minprice = DEFAULT_MIN;
        } try {
            maxprice  = Float.parseFloat(tickerMaxView.toString());
        } catch (NumberFormatException e) {
            maxprice = DEFAULT_MAX;
        }
        IntentStarter.startProductPickerActivity(this.getContext(),
                                                enabledRepositoryMap,
                                                chosenCategoriesList,
                                                tagsEditText.getText().toString(),
                                                maxprice,
                                                minprice,
                                                wishlistId,
                                                startingDisplayOrder);
    }

    private ArrayList<CategoryType> getChosenCategoriesFromUI(String ageSelected) {
        ArrayList<CategoryType> chosenCategoriesList = new ArrayList<>();
        String occasion = getPresenter().getWishlist(wishlistId).getOccasion();
        chosenCategoriesList.add(CategoryType.getCategoryTypeFromString(occasion));
        chosenCategoriesList.add(CategoryType.getCategoryTypeFromString(ageSelected));
        if (gamesCheckbox.isChecked()) {
            chosenCategoriesList.add(CategoryType.GAME);
        }
        if (handcraftCheckbox.isChecked()) {
            chosenCategoriesList.add(CategoryType.HANDCRAFT);
        }
        if (techCheckbox.isChecked()) {
            chosenCategoriesList.add(CategoryType.TECH);
        }
        if (sportsCheckbox.isChecked()) {
            chosenCategoriesList.add(CategoryType.SPORT);
        }
        if (travelCheckbox.isChecked()) {
            chosenCategoriesList.add(CategoryType.TRAVEL);
        }
        if (artCheckbox.isChecked()) {
            chosenCategoriesList.add(CategoryType.ART);
        }
        if (nerdCheckbox.isChecked()) {
            chosenCategoriesList.add(CategoryType.NERD);
        }
        if (bookCheckbox.isChecked()) {
            chosenCategoriesList.add(CategoryType.BOOK);
        }
        if (musicCheckbox.isChecked()) {
            chosenCategoriesList.add(CategoryType.MUSIC);
        }
        return chosenCategoriesList;
    }

    private void initBackgroundTransitions() {
        Resources resources = getResources();
        int firstBackgroundColor = ResourcesCompat.getColor(resources, R.color.material_purple_600, getContext().getTheme());
        int secondBackgroundColor = ResourcesCompat.getColor(resources, R.color.material_purple_500, getContext().getTheme());
        int thirdBackgroundColor = ResourcesCompat.getColor(resources, R.color.material_purple_400, getContext().getTheme());
        int fourthBackgroundColor = ResourcesCompat.getColor(resources, R.color.material_purple_300, getContext().getTheme());
        backgroundAnimator = ValueAnimator
                .ofObject(new ArgbEvaluator(), firstBackgroundColor, secondBackgroundColor, thirdBackgroundColor, fourthBackgroundColor);
        backgroundAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                coordinatorLayout.setBackgroundColor((int) animation.getAnimatedValue());
            }
        });
    }

    private void initPages() {
        coordinatorLayout.addPage(R.layout.page_product_picker_settings_first,
                R.layout.page_product_picker_settings_second,
                R.layout.page_product_picker_settings_third,
                R.layout.page_product_picker_settings_fourth);
    }

    private void initListeners() {
        coordinatorLayout.setOnPageScrollListener(new WelcomeCoordinatorLayout.OnPageScrollListener() {
            @Override
            public void onScrollPage(View v, float progress, float maximum) {
                if (!animationReady) {
                    animationReady = true;
                    backgroundAnimator.setDuration((long) maximum);
                }
                backgroundAnimator.setCurrentPlayTime((long) progress);
            }
            @Override
            public void onPageSelected(View v, int pageSelected) {
                if (pageSelected == MAX_PAGE) {
                    skipButton.setVisibility(View.INVISIBLE);
                    nextButton.setVisibility(View.INVISIBLE);
                    finishButton.setVisibility(View.VISIBLE);
                } else {
                    skipButton.setVisibility(View.VISIBLE);
                    nextButton.setVisibility(View.VISIBLE);
                    finishButton.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @OnClick(R.id.skip)
    void skip() {
        startProductActivity();
    }

    @OnClick(R.id.next)
    void next() {
        coordinatorLayout.setCurrentPage(coordinatorLayout.getPageSelected() + 1, true);
    }

    @OnClick(R.id.finish)
    void end() {
        startProductActivity();
    }

    public HashMap<Class,Boolean> getEnabledRepositoriesMap() {
        HashMap<Class,Boolean> enabledRepositoriesMap = new HashMap<>();
        if (etsyCheckbox.isChecked()) {
            enabledRepositoriesMap.put(EtsyProductDataSource.class, Boolean.TRUE);
        } else {
            enabledRepositoriesMap.put(EtsyProductDataSource.class, Boolean.FALSE);
        }
        if (ebayCheckbox.isChecked()) {
            enabledRepositoriesMap.put(EbayProductDataSource.class, Boolean.TRUE);
        } else {
            enabledRepositoriesMap.put(EbayProductDataSource.class, Boolean.FALSE);
        }
        return enabledRepositoriesMap;
    }
}
