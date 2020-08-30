# https://sciyoshi.com/2010/12/gray-codes/

def rotate_right(x, n):
    return x[-n:] + x[:-n]

def pi(n):
    if n <= 1:
        return (0,)
    x = pi(n - 1) + (n - 1,)
    return rotate_right(tuple(x[k] for k in x), 1)

def p(n, j, reverse=False):
    if n == 1 and j == 0:
        if not reverse:
            yield (0,)
            yield (1,)
        else:
            yield (1,)
            yield (0,)
    elif j >= 0 and j < n:
        perm = pi(n - 1)
        if not reverse:
            for x in p(n - 1, j - 1):# n-1 pieces of size n-1 ?
                yield (1,) + tuple(x[k] for k in perm)
            for x in p(n - 1, j):
                yield (0,) + x
        else:
            for x in p(n - 1, j, reverse=True):
                yield (0,) + x
            for x in p(n - 1, j - 1, reverse=True):
                yield (1,) + tuple(x[k] for k in perm)

def monotonic(n):
    for i in range(n):
        for x in (p(n, i) if i % 2 == 0 else p(n, i, reverse=True)):
            yield x


## then my stuff


def printMat(mat):
    print("\n".join(map(lambda x: " ".join(map(lambda y:str(y).rjust(2),x)), mat)))

n = 4
deps =  [0, 0, 0, 2, 2, 0, 5, 5, 6, 8, 9, 10, 10,13][:n]
leafs = [0 if i in deps else 1 for i in range(len(deps))]

def gaaa(x,curr,leafs,deps):
    for i in (list(range(len(curr))))[::-1]:
        if leafs[i]:
            j = sum(leafs[:i])-1
            curr[i] = x[j]
        curr[deps[i]] = curr[i] or curr[deps[i]]
    return curr

def gaaaConstr(curr, i,deps):
    for i in (list(range(len(curr))))[::-1]:
        curr[deps[i]] = curr[i] or curr[deps[i]]
    return curr

def faaa(n, prev, curr,deps):
    curr = list(curr)
    to_change = [0 for x in range(n)]
    res = []
    for i in (list(range(n)))[::-1]:
        curr = list(curr)
        # print(curr,prev,to_change,i)
        if to_change[i]==1:
            tmp = list(prev)
            tmp[i] = 1
            curr[i] = 1
            curr = gaaaConstr(curr,0,deps)
            res.append(gaaaConstr(tmp,0,deps))
        if curr[i]==1 and prev[i]==0 and prev[deps[i]]==0:
            to_change[deps[i]] = 1
        elif curr[i]==1 and prev[i]==0 and prev[deps[i]]==0 and to_change[deps[i]]==1:
            to_change[deps[i]] = 0

    return res[::-1]

already_found = []

def constrained(n):
    mono = monotonic(sum(leafs[:n]))
    prev = [0 for x in range(n)]
    ii = 0
    for x in mono:
        x = x[::-1]
        curr = [0 for a in range(n)]
        curr = gaaa(x,curr,leafs,deps)
        for aux_r in faaa(n, prev, curr,deps):
            print(aux_r,sum([1 for i in range(n) if aux_r[i]!=prev[i]]), aux_r in already_found, ii)
            yield (aux_r,sum([1 for i in range(n) if aux_r[i]!=prev[i]]), aux_r in already_found, ii)
            already_found.append(aux_r)
            prev = aux_r
            ii+=1
        print(curr,sum([1 for i in range(n) if curr[i]!=prev[i]]), curr in already_found, ii)
        yield (curr,sum([1 for i in range(n) if curr[i]!=prev[i]]), curr in already_found, ii)
        already_found.append(curr)
        prev = curr
        ii+=1
        # jj = 0
        # snd = curr
        # for i in (list(range(n))):
        #     if leafs[i]==0:
        #         tmp = []
        #         for k in (range(n)):
        #             if deps[n-k-1]==n-i-1:
        #                 if curr[n-k-1]:
        #                     tmp.append(1)
        #         #curr[i] = 1 if len(tmp)>0 else 0
        #         if len(tmp)<=0:
        #             #curr# TODO mat.append([a for a in mat[ii-1]])
        #             jj+=1
                    #mat[ii+jj][i] = 1
        # ii+=1+jj


print("res","changes","alr_found")
mat = list(constrained(n))
print()
printMat(mat)
print()
print(list(range(len(leafs))))
print(leafs)
print(deps)

depth = [0 for x in range(n)]
for i in range(n):
    for j in range(i,n):
        if deps[j]==i:
            depth[j] = depth[i]+1
depth = [x-1 for x in depth]
print(depth)


comb_size = [1 for x in deps]
total_comb_size = 1
for i in range(len(deps))[::-1]:
    comb_size[i]+=1
    if i != deps[i]:
        comb_size[deps[i]]*=comb_size[i]
    else:
        total_comb_size *=comb_size[i]

print(comb_size)
print(total_comb_size)
print(rotate_right([1,2,3],1))
# print(list(p(1,0,True)))
# print(list(p(2,0,True)))
# print(list(p(2,1,True)))
# print(list(p(3,0,True)))
# print(list(p(3,1,True)))
# print(list(p(3,2,True)))
# print(list(p(4,0,True)))
# print(list(p(4,1,True)))
# print(list(p(4,2,True)))
# print(list(p(4,3,True)))
print()
print()
printMat(monotonic(5))
print()
print(leafs)
print(deps)
print(len(deps))
print(gaaa([1,1],[0,0,0,0],[0,1,0,1],[0,0,0,2]))