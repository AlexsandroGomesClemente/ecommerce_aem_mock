export interface AemHeroBanner {
  jcrPath: string;
  eyebrow: string;
  title: string;
  subtitle: string;
  ctaLabel: string;
  themeAccent: string;
  authoredBy: string;
  lastPublished: string;
}

export interface AemProductEditorial {
  jcrPath: string;
  headline: string;
  richDescription: string;
  badge: string;
  highlights: string[];
  imageGradient: string;
  imageIcon: string;
  imageUrl: string;
  seoKeywords: string;
}

export interface ProductView {
  id: number;
  sku: string;
  name: string;
  category: string;
  price: number;
  stock: number;
  rating: number;
  aemContentPath: string;
  editorialContent: AemProductEditorial;
}

export interface StorefrontCatalogResponse {
  heroBanner: AemHeroBanner;
  products: ProductView[];
  totalProducts: number;
  dataSourceInfo: string;
}

export interface UserProfileView {
  id: number;
  name: string;
  email: string;
  role: string;
  planTier: string;
  authenticatedVia: string;
}

export interface AuthStatusResponse {
  authenticated: boolean;
  message: string;
  cookiePolicy: string;
  user: UserProfileView | null;
}

export interface CartItem {
  product: ProductView;
  quantity: number;
}

export interface OrderItemView {
  productSku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface OrderView {
  id: number;
  orderCode: string;
  userEmail: string;
  totalAmount: number;
  status: string;
  paymentMethod: string;
  createdAt: string;
  items: OrderItemView[];
}

export interface StructuredLogEntry {
  timestamp: string;
  correlationId: string;
  level: string;
  component: string;
  message: string;
  latencyMs: number;
}

export interface TelemetrySnapshot {
  totalRequests: number;
  graphqlRequests: number;
  restRequests: number;
  ordersCreated: number;
  avgLatencyMs: number;
  p95LatencyMs: number;
  activeDenyListTokens: number;
  recentLogs: StructuredLogEntry[];
}
