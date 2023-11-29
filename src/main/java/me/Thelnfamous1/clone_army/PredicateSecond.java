package me.Thelnfamous1.clone_army;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class PredicateSecond<T, U> implements Predicate<U> {

    private final T first;
    private final BiPredicate<T, U> biPredicate;

    public PredicateSecond(T first, BiPredicate<T, U> biPredicate){
        this.first = first;
        this.biPredicate = biPredicate;
    }

    public BiPredicate<T, U> getBiPredicate() {
        return this.biPredicate;
    }

    @Override
    public boolean test(U t) {
        return this.biPredicate.test(this.first, t);
    }
}
