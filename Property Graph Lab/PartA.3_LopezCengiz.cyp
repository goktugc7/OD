// Add 10 universities
FOREACH (i in range(1, 10) | CREATE (:university {id: i, name: 'university_' + i} ));


// Add 10 companies
FOREACH (i in range(1, 10) | CREATE (:company {id: i, name: 'company_' + i} ));


// Assign the authors to a company or university
MATCH (u:university), (a:author)
WHERE a.id%2=0 and u.id = ceil(a.id/100.0)
CREATE (a) -[:belongs_to]-> (u);

MATCH (c:company), (a:author)
WHERE a.id%2=1 and c.id = ceil(a.id/100.0)
CREATE (a) -[:belongs_to]-> (c);


// Add acceptance or rejection to reviews and transform it into a new node
MATCH (a:author) -[r:reviews]-> (p:paper)
DELETE r
CREATE (a) -[:does]->(:review {comments:'bla bla bla', approved: rand()>0.5}) -[:of]-> (p);

