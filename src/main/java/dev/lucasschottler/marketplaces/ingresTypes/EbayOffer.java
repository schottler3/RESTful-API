package dev.lucasschottler.marketplaces.ingresTypes;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
public class EbayOffer {
    private int availableQuantity;
    private String categoryId;
    private Charity charity;
    private ExtendedProducerResponsibility extendedProducerResponsibility;
    private String format;
    private boolean hideBuyerDetails;
    private boolean includeCatalogProductDetails;
    private Listing listing;
    private String listingDescription;
    private String listingDuration;
    private ListingPolicies listingPolicies;
    private String listingStartDate;
    private int lotSize;
    private String marketplaceId;
    private String merchantLocationKey;
    private String offerId;
    private PricingSummary pricingSummary;
    private int quantityLimitPerBuyer;
    private Regulatory regulatory;
    private String secondaryCategoryId;
    private String sku;
    private String status;
    private List<String> storeCategoryNames;
    private Tax tax;

    public String getListingId() {
        return listing != null ? listing.getListingId() : null;
    }

    public String getListingStatus() {
        return listing != null ? listing.getListingStatus() : null;
    }

    public boolean isListingOnHold() {
        return listing != null && listing.isListingOnHold();
    }

    public int getSoldQuantity() {
        return listing != null ? listing.getSoldQuantity() : 0;
    }

    @Data
    public static class Charity {
        private String charityId;
        private String donationPercentage;
    }

    @Data
    public static class ExtendedProducerResponsibility {
        private MoneyAmount ecoParticipationFee;
        private String producerProductId;
        private String productDocumentationId;
        private String productPackageId;
        private String shipmentPackageId;
    }

    @Data
    public static class Listing {
        private String listingId;
        private boolean listingOnHold;
        private String listingStatus;
        private int soldQuantity;
    }

    @Data
    public static class ListingPolicies {
        private BestOfferTerms bestOfferTerms;
        private boolean eBayPlusIfEligible;
        private String fulfillmentPolicyId;
        private String paymentPolicyId;
        private List<String> productCompliancePolicyIds;
        private RegionalPolicies regionalProductCompliancePolicies;
        private RegionalPolicies regionalTakeBackPolicies;
        private String returnPolicyId;
        private List<ShippingCostOverride> shippingCostOverrides;
        private String takeBackPolicyId;
    }

    @Data
    public static class BestOfferTerms {
        private MoneyAmount autoAcceptPrice;
        private MoneyAmount autoDeclinePrice;
        private boolean bestOfferEnabled;
    }

    @Data
    public static class RegionalPolicies {
        private List<CountryPolicy> countryPolicies;
    }

    @Data
    public static class CountryPolicy {
        private String country;
        private List<String> policyIds;
    }

    @Data
    public static class ShippingCostOverride {
        private MoneyAmount additionalShippingCost;
        private int priority;
        private MoneyAmount shippingCost;
        private String shippingServiceType;
        private MoneyAmount surcharge;
    }

    @Data
    public static class PricingSummary {
        private MoneyAmount auctionReservePrice;
        private MoneyAmount auctionStartPrice;
        private MoneyAmount minimumAdvertisedPrice;
        private String originallySoldForRetailPriceOn;
        private MoneyAmount originalRetailPrice;
        private MoneyAmount price;
        private String pricingVisibility;
    }

    @Data
    public static class Regulatory {
        private List<Document> documents;
        private EnergyEfficiencyLabel energyEfficiencyLabel;
        private Hazmat hazmat;
        private Address manufacturer;
        private ProductSafety productSafety;
        private double repairScore;
        private List<ResponsiblePerson> responsiblePersons;
    }

    @Data
    public static class Document {
        private String documentId;
    }

    @Data
    public static class EnergyEfficiencyLabel {
        private String imageDescription;
        private String imageURL;
        private String productInformationSheet;
    }

    @Data
    public static class Hazmat {
        private String component;
        private List<String> pictograms;
        private String signalWord;
        private List<String> statements;
    }

    @Data
    public static class Address {
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String companyName;
        private String contactUrl;
        private String country;
        private String email;
        private String phone;
        private String postalCode;
        private String stateOrProvince;
    }

    @Data
    public static class ProductSafety {
        private String component;
        private List<String> pictograms;
        private List<String> statements;
    }

    @Data
    @EqualsAndHashCode(callSuper=false)
    public static class ResponsiblePerson extends Address {
        private List<String> types;
    }

    @Data
    public static class Tax {
        private boolean applyTax;
        private String thirdPartyTaxCategory;
        private double vatPercentage;
    }

    @Data
    public static class MoneyAmount {
        private String currency;
        private String value;
    }

    @Data
    public static class EbayOffersResponse {
        private List<EbayOffer> offers;
        private int total;
        private int size;
        private int offset;
        private int limit;
    }
}