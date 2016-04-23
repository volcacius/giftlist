package it.polimi.dima.giftlist.domain.interactor;

import com.fernandocejas.frodo.annotation.RxLogObservable;

import org.greenrobot.eventbus.EventBus;

import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import it.polimi.dima.giftlist.data.model.Currency;
import it.polimi.dima.giftlist.data.model.EbayProduct;
import it.polimi.dima.giftlist.data.model.Product;
import it.polimi.dima.giftlist.data.repository.datasource.EbayProductDataSource;
import it.polimi.dima.giftlist.domain.repository.CurrencyRepository;
import it.polimi.dima.giftlist.domain.repository.ProductRepository;
import rx.Observable;
import rx.functions.Func2;

/**
 * Created by Elena on 27/01/2016.
 */
public class GetProductListUseCase extends UseCase<List<Product>> {

    private static final int PRODUCT_PER_PAGE = 25;
    private static final int DIGITS = 2;
    private static final int STARTING_OFFSET = 0;
    private List<ProductRepository<Product>> productRepositoryList;
    private CurrencyRepository currencyRepository;
    private String category;
    private String keywords;
    private int searchOffset;
    protected EventBus eventBus;

    @Inject
    public GetProductListUseCase(List<ProductRepository<Product>> productRepositoryList,
                                 CurrencyRepository currencyRepository,
                                 String category,
                                 String keywords,
                                 EventBus eventBus) {
        this.currencyRepository = currencyRepository;
        this.productRepositoryList = productRepositoryList;
        this.category = category;
        this.keywords = keywords;
        this.searchOffset = STARTING_OFFSET;
        this.eventBus = eventBus;
    }

    @RxLogObservable
    @Override
    //At the end of the chain I need to wrap the product as a single valued list, since list<product> is the type accepted as model accross the whole use case
    protected Observable<List<Product>> buildUseCaseObservable() {
        List<Observable<List<Product>>> productListObservableList = new ArrayList<>();
        for (ProductRepository<Product> pr : productRepositoryList) {
            productListObservableList.add(pr.getProductList(category, keywords, searchOffset*PRODUCT_PER_PAGE));
        }
        Observable<List<Currency>> currencyList = currencyRepository.getCurrencyList();
        searchOffset++;
        return Observable.merge(productListObservableList)
                .flatMap(products -> Observable.from(products))
                .withLatestFrom(currencyList, new Func2<Product, List<Currency>, Product>() {
                    @Override
                    public Product call(Product product, List<Currency> currencies) {
                        if (product.getClass().equals(EbayProduct.class)) {
                            product.setImageUrl(EbayProductDataSource.getHQImageUrl((EbayProduct) product));
                        }
                        for (Currency c : currencies) {
                            if (c.getCurrencyType().equals(product.getCurrencyType())) {
                                product.setConvertedPrice(round(product.getPrice() / c.getRate(), DIGITS));
                            }
                        }
                        return product;
                    }
        }).map(product -> new ArrayList<Product>(Arrays.asList(product)));
    }

    private float round(float value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.floatValue();
    }
}
