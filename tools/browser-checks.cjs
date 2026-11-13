/* Run only through Run-IsolatedBrowserChecks.ps1 against its disposable demo. */
const { chromium } = require('playwright-core');
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const statePath = process.env.GAMEHUB_BROWSER_STATE || path.resolve('target/final-audit/state.json');
const state = JSON.parse(fs.readFileSync(statePath, 'utf8').replace(/^\uFEFF/, ''));
assert.match(state.container, /^gamehub-(final|demo)-[a-z0-9]+$/);
const base = 'http://127.0.0.1:18087', checks = [], metrics = [], screenshots = [];
const capture = process.env.GAMEHUB_CAPTURE_SCREENSHOTS === 'true';
const pass = name => { checks.push(name); console.log('PASS ' + name); };

(async () => {
 const browser = await chromium.launch({headless:true, executablePath:process.env.GAMEHUB_EDGE_PATH || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
 try {
  const options = {viewport:{width:1440,height:1000},deviceScaleFactor:1};
  const admin=await browser.newContext(options), a=await browser.newContext(options), b=await browser.newContext(options);
  const panel=await admin.newPage(), page=await a.newPage(), other=await b.newPage();
  const errors=[];for(const p of [panel,page,other])p.on('pageerror',error=>errors.push(error.message));
  async function go(p,route) { const r=await p.goto(base+route);assert.equal(r.status(),200,route); }
  async function screenshot(p,file) {
   if(!capture)return;
   await p.evaluate(()=>document.fonts.ready);
   const output=path.resolve('docs/screenshots',file);fs.mkdirSync(path.dirname(output),{recursive:true});
   const footer=p.locator('footer'); const box=await footer.count()?await footer.boundingBox():null;
   await p.screenshot({path:output,fullPage:true,...(box?{clip:{x:0,y:0,width:p.viewportSize().width,height:Math.floor(box.y)}}:{})});
   screenshots.push(file);
  }
  async function layout(p,route,width,label) {
   await p.setViewportSize({width,height:844});await go(p,route);
   const result=await p.evaluate(()=>({viewport:innerWidth,content:document.documentElement.scrollWidth,
    navigation:[...document.querySelectorAll('nav a,nav button')].map(e=>e.getBoundingClientRect()).filter(r=>r.width>0).every(r=>r.left>=-1&&r.right<=innerWidth+1)}));
   metrics.push({page:label,width,...result});assert.ok(result.content<=width+1,`${label} ${width}px overflow: ${result.content}`);
   assert.ok(result.navigation,`${label} ${width}px navigation outside viewport`);pass(`responsive ${label} ${width}px`);
  }
  async function login(p) {
   await go(p,'/admin/login');await p.fill('#email',state.adminEmail);await p.fill('#password',state.adminPassword);
   await Promise.all([p.waitForURL(base+'/admin'),p.locator('button[type=submit]').click()]);
  }
  const protectedPage=await b.request.get(base+'/admin',{maxRedirects:0});
  assert.equal(protectedPage.status(),302);assert.match(protectedPage.headers().location,/\/admin\/login$/);
  pass('anonymous administrator dashboard rejected');
  await go(other,'/admin/login');
  const anonymousCsrf=await other.locator('[name=_csrf]').inputValue();
  const protectedWrite=await b.request.post(base+'/admin/save-product',{
   form:{_csrf:anonymousCsrf,nom:'Unauthorized fixture',prix:'1.00',quantite:'1'},maxRedirects:0});
  assert.equal(protectedWrite.status(),302);assert.match(protectedWrite.headers().location,/\/admin\/login$/);
  pass('anonymous administrator write rejected with valid CSRF');
  const legacyWrite=await b.request.post(base+'/saveCommandes',{form:{_csrf:anonymousCsrf},maxRedirects:0});
  assert.equal(legacyWrite.status(),302);assert.match(legacyWrite.headers().location,/\/admin\/login$/);
  pass('legacy financial write rejected');
  await login(panel);pass('administrator login');
  async function category(name) {
   await go(panel,'/admin/add-category');await panel.fill('#nom',name);await panel.fill('#description','Catalogue fictif de démonstration.');
   await Promise.all([panel.waitForURL(base+'/admin'),panel.locator('button[type=submit]').click()]);
   return panel.locator('tr').filter({has:panel.getByText(name,{exact:true})}).locator('td').first().innerText();
  }
  const keyboards=await category('Claviers'),mice=await category('Souris');
  async function product(name,categoryId,price,stock,description) {
   await go(panel,'/admin/add-product');await panel.fill('#nom',name);await panel.fill('#description',description);
   await panel.fill('#prix',price);await panel.fill('#quantite',stock);await panel.selectOption('#categorie',categoryId);
   await Promise.all([panel.waitForURL(base+'/admin'),panel.locator('button[type=submit]').click()]);
   const href=await panel.locator('tr').filter({has:panel.getByText(name,{exact:true})}).locator('a[href*="edit-product"]').getAttribute('href');
   return new URL(href,base).searchParams.get('id');
  }
  const mainId=await product('Clavier mécanique RGB',keyboards,'899.90','12','Switchs mécaniques, éclairage RGB et format compact. Démonstration.');
  await product('Souris gaming légère',mice,'449.90','12','Capteur optique précis et prise en main confortable. Démonstration.');
  await product('Clavier compact sans fil',keyboards,'1099.90','12','Connexion sans fil et touches programmables. Démonstration.');
  const temporary=await product('Produit temporaire',keyboards,'10.00','2','Vérification des opérations administrateur.');
  await go(panel,'/admin/edit-product?id='+temporary);await panel.fill('#nom','Produit temporaire modifié');
  await Promise.all([panel.waitForURL(base+'/admin'),panel.locator('button[type=submit]').click()]);
  panel.once('dialog',dialog=>dialog.accept());
  await panel.locator(`form[action*="/admin/delete-product?id=${temporary}"] button`).click();
  assert.equal(await panel.getByText('Produit temporaire modifié',{exact:true}).count(),0);pass('administrator product create edit delete');
  const emptyCategory=await category('Catégorie temporaire');panel.once('dialog',dialog=>dialog.accept());
  await panel.locator(`form[action*="/admin/delete-category?id=${emptyCategory}"] button`).click();
  assert.equal(await panel.getByText('Catégorie temporaire',{exact:true}).count(),0);pass('administrator category create delete');
  await go(page,'/');await screenshot(page,'desktop/home.png');pass('public home');
  await page.locator('a[href="/produits"]').first().click();assert.equal(new URL(page.url()).pathname,'/produits');
  assert.equal(await page.locator('.product-card').count(),3);await screenshot(page,'desktop/catalogue.png');pass('public navigation and catalogue');
  await go(page,'/produits?search=Souris');assert.equal(await page.locator('.product-card').count(),1);pass('product search');
  await go(page,'/produits?categoryId='+keyboards);assert.equal(await page.locator('.product-card').count(),2);pass('category filter');
  await go(page,'/produits?search=Clavier%20mécanique');
  await Promise.all([page.waitForURL(base+'/panier'),page.locator('.product-card button').click()]);
  await go(other,'/panier');assert.equal(await other.locator('.empty-cart').count(),1);pass('independent visitor basket');
  const remove=await page.locator('form[action*="/panier/supprimer/"]').getAttribute('action');
  await go(other,'/admin/login');let otherCsrf=await other.locator('[name=_csrf]').inputValue();
  let r=await b.request.post(base+remove,{form:{_csrf:otherCsrf},maxRedirects:0});assert.equal(r.status(),404);pass('foreign cart line rejected');
  r=await a.request.post(base+'/panier/vider',{maxRedirects:0});assert.equal(r.status(),403);pass('cart CSRF');
  await go(panel,'/admin/edit-product?id='+mainId);const staleVersion=await panel.locator('[name=version]').inputValue();
  await page.locator('a[href="/checkout"]').click();
  await page.fill('#prenom','Client');await page.fill('#nom','Fictif');await page.fill('#email','buyer@example.invalid');await page.fill('#adresse','Adresse fictive de démonstration');
  const token=await page.locator('[name=checkoutToken]').inputValue();const csrf=await page.locator('[name=_csrf]').inputValue();
  const contact={prenom:'Client',nom:'Fictif',email:'buyer@example.invalid',adresse:'Adresse fictive de démonstration',checkoutToken:token,_csrf:csrf};
  await Promise.all([page.waitForURL(/\/paiement\/\d+$/),page.locator('form[action="/commander"] button').click()]);
  const orderId=new URL(page.url()).pathname.split('/').pop();assert.equal((await page.locator('.price-val').innerText()).trim(),'899.90 DH');pass('exact decimal checkout');
  r=await a.request.post(base+'/commander',{form:contact,maxRedirects:0});assert.equal(r.status(),302);assert.ok(r.headers().location.endsWith('/paiement/'+orderId));pass('durable same-key replay');
  await a.clearCookies({name:'JSESSIONID'});await go(page,'/checkout');const recoveredCsrf=await page.locator('[name=_csrf]').inputValue();
  contact._csrf=recoveredCsrf;r=await a.request.post(base+'/commander',{form:contact,maxRedirects:0});assert.equal(r.status(),302);
  assert.ok(r.headers().location.endsWith('/paiement/'+orderId));pass('durable replay after a new HTTP session');
  const adminCsrf=await panel.locator('[name=_csrf]').inputValue();
  r=await admin.request.post(base+'/admin/update-product',{form:{_csrf:adminCsrf,id:mainId,version:staleVersion,nom:'Clavier mécanique RGB',prix:'899.90',quantite:'999'},maxRedirects:0});
  assert.equal(r.status(),409);pass('stale administrator stock update rejected');
  for(const route of ['/paiement/','/confirmation/']){r=await b.request.get(base+route+orderId);assert.equal(r.status(),404);}pass('foreign order consultation rejected');
  r=await b.request.post(base+'/commander/payer',{form:{_csrf:otherCsrf,commandeId:orderId,methode:'CASH'},maxRedirects:0});assert.equal(r.status(),404);pass('foreign order payment rejected');
  r=await a.request.post(base+'/commander/payer',{form:{_csrf:recoveredCsrf,commandeId:orderId,methode:'INVALID'},maxRedirects:0});assert.equal(r.status(),400);pass('invalid payment method rejected');
  await go(page,'/confirmation/'+orderId);assert.ok((await page.locator('body').innerText()).includes('En attente de paiement'));pass('truthful pending status');
  await go(page,'/paiement/'+orderId);await page.selectOption('#methode','CASH');
  await Promise.all([page.waitForURL(base+'/confirmation/'+orderId),page.locator('form[action="/commander/payer"] button').click()]);
  assert.ok((await page.locator('body').innerText()).includes('Confirmée'));pass('simulated payment and confirmation');
  const repeats=await Promise.all(Array.from({length:4},()=>a.request.post(base+'/commander/payer',{form:{_csrf:recoveredCsrf,commandeId:orderId,methode:'CASH'},maxRedirects:0})));
  repeats.forEach(response=>assert.equal(response.status(),302));pass('concurrent payment repetitions');
  await go(panel,'/admin');await screenshot(panel,'desktop/admin-dashboard.png');
  const stockRow=panel.locator('tr').filter({has:panel.getByText('Clavier mécanique RGB',{exact:true})});assert.equal((await stockRow.locator('td').nth(4).innerText()).trim(),'11');pass('stock deducted once');
  const mobileContext=await browser.newContext(options), mobile=await mobileContext.newPage();mobile.on('pageerror',error=>errors.push(error.message));
  await go(mobile,'/produits?search=Souris');await Promise.all([mobile.waitForURL(base+'/panier'),mobile.locator('.product-card button').click()]);
  for(const width of [390,320]) {
   await layout(mobile,'/',width,'home');await screenshot(mobile,`mobile/home-${width}.png`);
   await layout(mobile,'/produits',width,'catalogue');await screenshot(mobile,`mobile/catalogue-${width}.png`);
   await layout(mobile,'/panier',width,'cart');await screenshot(mobile,`mobile/cart-${width}.png`);
   await layout(mobile,'/checkout',width,'checkout');
   await layout(other,'/admin/login',width,'login');
   await layout(panel,'/admin',width,'admin');await screenshot(panel,`mobile/admin-${width}.png`);
   await layout(panel,'/admin/add-product',width,'add product');
   await layout(panel,'/admin/edit-product?id='+mainId,width,'edit product');
   await layout(panel,'/admin/add-category',width,'add category');
  }
  await go(mobile,'/checkout');await mobile.fill('#prenom','Client');await mobile.fill('#nom','Mobile');await mobile.fill('#email','mobile@example.invalid');await mobile.fill('#adresse','Adresse mobile fictive');
  await Promise.all([mobile.waitForURL(/\/paiement\/\d+$/),mobile.locator('form[action="/commander"] button').click()]);
  const pendingId=new URL(mobile.url()).pathname.split('/').pop();
  for(const width of [390,320])await layout(mobile,'/paiement/'+pendingId,width,'payment');
  await Promise.all([mobile.waitForURL(base+'/confirmation/'+pendingId),mobile.locator('form[action="/commander/annuler"] button').click()]);
  assert.ok((await mobile.locator('body').innerText()).includes('Annulée'));pass('pending cancellation restores reservation');
  for(const width of [390,320])await layout(mobile,'/confirmation/'+pendingId,width,'confirmation');
  await go(panel,'/admin');await Promise.all([panel.waitForURL(/\/admin\/login\?logout$/),panel.locator('form[action="/admin/logout"] button').click()]);
  await panel.goto(base+'/admin');assert.equal(new URL(panel.url()).pathname,'/admin/login');pass('administrator logout');
  assert.deepEqual(errors,[]);pass('no browser JavaScript exceptions');
  fs.writeFileSync(path.join(path.dirname(statePath),'browser-results.json'),JSON.stringify({checks,metrics,screenshots,browser:browser.version()},null,2));
 } finally {await browser.close();}
})().catch(error=>{console.error(error.message);process.exitCode=1;});
