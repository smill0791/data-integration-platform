const express = require('express');
const cors = require('cors');
const { faker } = require('@faker-js/faker');

const app = express();
const PORT = 3003;

app.use(cors());
app.use(express.json());

const statuses = ['paid', 'pending', 'overdue'];
const currencies = ['USD', 'EUR', 'GBP', 'CAD'];

// Generate 60 fake invoices at startup
const invoices = Array.from({ length: 60 }, (_, i) => {
  const lineItemCount = faker.number.int({ min: 1, max: 5 });
  const lineItems = Array.from({ length: lineItemCount }, () => ({
    description: faker.commerce.productName(),
    quantity: faker.number.int({ min: 1, max: 10 }),
    unitPrice: parseFloat(faker.commerce.price({ min: 10, max: 1000, dec: 2 })),
  }));

  const amount = lineItems.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0);

  return {
    id: `ACC-${String(i + 1).padStart(3, '0')}`,
    invoiceNumber: `INV-${faker.string.numeric(6)}`,
    customerName: faker.company.name(),
    amount: parseFloat(amount.toFixed(2)),
    currency: faker.helpers.arrayElement(currencies),
    status: faker.helpers.arrayElement(statuses),
    dueDate: faker.date.soon({ days: 90 }).toISOString().split('T')[0],
    lineItems,
  };
});

console.log(`Generated ${invoices.length} fake invoices`);

// Middleware: random delay 100-500ms
function randomDelay(req, res, next) {
  const delay = Math.floor(Math.random() * 400) + 100;
  setTimeout(next, delay);
}

// Middleware: 5% random failure rate
function randomFailure(req, res, next) {
  if (Math.random() < 0.05) {
    return res.status(500).json({
      error: 'Internal Server Error',
      message: 'Simulated random failure',
    });
  }
  next();
}

app.use(randomDelay);
app.use(randomFailure);

// GET /api/invoices - paginated list
app.get('/api/invoices', (req, res) => {
  const page = parseInt(req.query.page) || 0;
  const size = parseInt(req.query.size) || 20;

  const start = page * size;
  const end = start + size;
  const content = invoices.slice(start, end);
  const totalElements = invoices.length;
  const totalPages = Math.ceil(totalElements / size);

  res.json({
    content,
    page,
    size,
    totalElements,
    totalPages,
  });
});

// GET /api/invoices/:id - single invoice
app.get('/api/invoices/:id', (req, res) => {
  const invoice = invoices.find((inv) => inv.id === req.params.id);
  if (!invoice) {
    return res.status(404).json({
      error: 'Not Found',
      message: `Invoice ${req.params.id} not found`,
    });
  }
  res.json(invoice);
});

app.listen(PORT, () => {
  console.log(`Mock Accounting API running on http://localhost:${PORT}`);
  console.log(`Try: http://localhost:${PORT}/api/invoices?page=0&size=20`);
});
